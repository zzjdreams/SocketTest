package com.demo.utils

import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class SocketUtils {
    inner class Service: SocketStub() {

        private var serverSocket: ServerSocket ?= null
        private var serviceSocketListener: ServerSocketListener? = null

        fun setServiceSocketListener(serviceSocketListener: ServerSocketListener) {
            this.serviceSocketListener = serviceSocketListener
        }

        fun createServerSocket(port: Int) {
            try {
                serverSocket = ServerSocket(port)
                socket = serverSocket?.accept()
                serviceSocketListener?.onCreateSuccess(serverSocket!!)
                start()
            }catch (e: Exception) {
                e.printStackTrace()
                serviceSocketListener?.onCreateFailure()
                serverSocket?.close()
                socket?.close()
            }
        }

    }

    inner class Client: SocketStub() {
        private var clientSocketListener: ClientSocketListener? = null

        fun setClientSocketListener(clientSocketListener: ClientSocketListener) {
            this.clientSocketListener = clientSocketListener
        }

        fun connectServer(ip: String, port: Int) {
            try {
                socket = Socket(ip, port)
                clientSocketListener?.onConnectSuccess()
                start()
            }catch (e: Exception) {
                e.printStackTrace()
                clientSocketListener?.onConnectFailure()
                socket?.close()
            }

        }
    }

    open class SocketStub{
        protected var socket: Socket ?= null
        private var socketMsgListener: SocketMessageListener? = null
        private var startReceive = false

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
                    val writer = DataOutputStream(socket?.getOutputStream())
                    writer.writeUTF(msg) // 写一个UTF-8的信息
                    socketMsgListener?.onSentSuccess()

                } catch (e: IOException) {
                    e.printStackTrace()
                    socketMsgListener?.onSentFailure()
                }
            }
        }

        private suspend fun sendMsgByThread(key: String, msg: Any){
            withContext(Dispatchers.IO) {
                try {
                    val writer = DataOutputStream(socket?.getOutputStream())
                    writer.writeUTF(key)
                    when(msg) {
                        is Int -> writer.write(msg)
                        is String -> writer.writeUTF(msg)
                        is Float -> writer.writeFloat(msg)
                        is Double -> writer.writeDouble(msg)
                        is ByteArray -> writer.write(msg)
                    }
                    socketMsgListener?.onSentSuccess()

                } catch (e: IOException) {
                    e.printStackTrace()
                    socketMsgListener?.onSentFailure()
                }
            }
        }

        private suspend fun startReceiver() {
            withContext(Dispatchers.IO) {
                try {
                    val reader = DataInputStream(socket?.getInputStream())
                    while (startReceive) {
                        val key  = reader.readUTF()
                        val msg = reader.readBytes()
                        socketMsgListener?.onReceived(key, msg)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun start() {
            if (startReceive) return
            startReceive = true
            MainScope().launch {
                startReceiver()
            }
        }

        fun stop() {
            startReceive = false
        }

        fun destroy() {
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