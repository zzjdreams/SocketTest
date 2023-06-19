package com.demo.utils

import android.text.TextUtils
import kotlinx.coroutines.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class SocketUtils {
    class Server(): SocketStub() {

        private var serverSocket: ServerSocket ?= null
        private var serviceSocketListener: ServerSocketListener? = null

        fun setServiceSocketListener(serviceSocketListener: ServerSocketListener) {
            this.serviceSocketListener = serviceSocketListener
        }

        fun createServerSocket(port: Int) {
            try {
                CoroutineScope (SupervisorJob() +Dispatchers.IO).launch {
                    withContext(Dispatchers.IO) {
                        serverSocket = ServerSocket(port)
                        while (true) {
                            socket = serverSocket?.accept()
                            serviceSocketListener?.onCreateSuccess(serverSocket!!)
                            start()
                        }
                    }
                }

            }catch (e: Exception) {
                e.printStackTrace()
                serviceSocketListener?.onCreateFailure()
                serverSocket?.close()
                socket?.close()
            }
        }

        override fun destroy() {
            try {
                serverSocket?.close()
            }catch (e: IOException) {
                e.printStackTrace()
            }

        }

    }

    class Client: SocketStub() {
        private var clientSocketListener: ClientSocketListener? = null

        fun setClientSocketListener(clientSocketListener: ClientSocketListener) {
            this.clientSocketListener = clientSocketListener
        }

        fun connectServer(ip: String, port: Int) {
            try {
                CoroutineScope (SupervisorJob() +Dispatchers.IO).launch {
                    withContext(Dispatchers.IO) {
                        socket = Socket(ip, port)
                        clientSocketListener?.onConnectSuccess()
                        start()
                    }
                }

            }catch (e: Exception) {
                e.printStackTrace()
                clientSocketListener?.onConnectFailure()
                socket?.close()
            }

        }
    }

    abstract class SocketStub{
        protected var socket: Socket ?= null
        private var socketMsgListener: SocketMessageListener? = null
        private var startReceive = false
        private var writer: DataOutputStream ?= null
        private var reader: DataInputStream ?= null

        fun setMsgListener(msgListener: SocketMessageListener) {
            this.socketMsgListener = msgListener
        }

        fun sendMsg(msg: String) {
            if (socket == null) throw NullPointerException()
            if (TextUtils.isEmpty(msg)) return
            MainScope().launch {
                sendMsgByThread(msg)
            }
        }

        fun sendMsgByAny(key: String, msg: Any) {
            MainScope().launch {
                sendMsgByThread(key, msg)
            }
        }

        private suspend fun sendMsgByThread(msg: String){
            withContext(Dispatchers.IO) {
                try {
                    writer?.writeUTF(msg) // 写一个UTF-8的信息
                    writer?.flush()
                    socketMsgListener?.onSentSuccess()

                } catch (e: IOException) {
                    e.printStackTrace()
                    socketMsgListener?.onSentFailure()
                }
            }
        }

        private fun sendMsgByThread(key: String, msg: Any){
            Thread(Runnable {
                try {
                    socket?.let {
                        writer?.writeUTF(key)
                        when(msg) {
                            is Int -> writer?.write(msg)
                            is String -> writer?.writeUTF(msg)
                            is Float -> writer?.writeFloat(msg)
                            is Double -> writer?.writeDouble(msg)
                            is ByteArray -> writer?.write(msg)
                        }
                        writer?.flush()
                        socketMsgListener?.onSentSuccess()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    close()
                    socketMsgListener?.onSentFailure()
                }
            }).start()
        }

        private suspend fun startReceiver() {
            withContext(Dispatchers.IO) {
                writer = DataOutputStream(socket?.getOutputStream())
                reader = DataInputStream(socket!!.getInputStream())
                try {
                    startReceive = true
                    while (startReceive) {
                        val key  = reader!!.readUTF()
                        val msg = reader!!.readUTF()
                        socketMsgListener?.onReceived(key, msg.toByteArray())

                    }
                } catch (e: IOException) {
                    startReceive = false
                    e.printStackTrace()
                    close()
                }
            }
        }

        fun start() {
            GlobalScope.launch {
                startReceiver()
            }
        }

        fun stop() {

        }

        private fun close() {
            reader?.close()
            writer?.close()
        }

        open fun destroy() {
            try {
                socket?.close()
            }catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }
}

interface ServerSocketListener {
    fun onCreateSuccess(serverSocket: ServerSocket)
    fun onCreateFailure()
}

interface ClientSocketListener {
    fun onConnectSuccess()
    fun onConnectFailure()
}

interface SocketMessageListener {
    fun onSentSuccess()
    fun onSentFailure()

    fun onReceived(key: String, msg: ByteArray)
}