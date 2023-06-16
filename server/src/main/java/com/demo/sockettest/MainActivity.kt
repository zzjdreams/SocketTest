package com.demo.sockettest

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.demo.sockettest.databinding.ActivityMainBinding
import com.demo.utils.ServerSocketListener
import com.demo.utils.SocketMessageListener
import com.demo.utils.SocketUtils
import java.net.ServerSocket

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var server: SocketUtils.Server

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
    }

    private fun initData() {
        refreshIp(binding.btnRefresh)
        server = SocketUtils.Server()
        server.setServiceSocketListener(object : ServerSocketListener{
            override fun onCreateSuccess(serverSocket: ServerSocket) {
                runOnUiThread {
                    toast("创建成功")
                    addMsg(">>创建成功<<")
                }
            }

            override fun onCreateFailure() {
                runOnUiThread {
                    toast("创建失败")
                    addMsg(">>创建失败<<")
                }
            }
        })
        server.setMsgListener(object : SocketMessageListener{
            override fun onSentSuccess() {
                runOnUiThread {
                    toast("发送成功")
                    addMsg(">>发送成功<<")
                }
            }

            override fun onSentFailure() {
                runOnUiThread {
                    toast("发送失败")
                    addMsg(">>发送失败<<")
                }
            }

            override fun onReceived(key: String, msg: ByteArray) {
                runOnUiThread {
                    addMsg("{key: $key, msg: ${String(msg, Charsets.UTF_8)}}")
                }

            }

        })
        server.createServerSocket(5959)

        binding.btnSend.setOnClickListener {
            server.sendMsgByAny("msg", binding.etSend.text.toString())
        }
    }

    private fun addMsg(msg: String) {
        binding.tvMsg.append("\n")
        binding.tvMsg.append(msg)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun getLocalIpAddress(context: Context): String {
        return try {
            val wifiManager = context
                .getSystemService(WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val i = wifiInfo.ipAddress
            int2ip(i)
        } catch (ex: Exception) {
            " 获取IP出错鸟!!!!请保证是WIFI,或者请重新打开网络!${ex.message}"
        }
    }

    override fun onDestroy() {
        server.destroy()
        super.onDestroy()
    }

    /**
     * 将ip的整数形式转换成ip形式
     *
     * @param ipInt
     * @return
     */
    fun int2ip(ipInt: Int): String {
        val sb = StringBuilder()
        sb.append(ipInt and 0xFF).append(".")
        sb.append(ipInt shr 8 and 0xFF).append(".")
        sb.append(ipInt shr 16 and 0xFF).append(".")
        sb.append(ipInt shr 24 and 0xFF)
        return sb.toString()
    }

    fun refreshIp(view: View) {
        binding.tvIpShow.text = getLocalIpAddress(this)
    }
}