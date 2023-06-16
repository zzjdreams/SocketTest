package com.demo.client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.demo.client.databinding.ActivityMainBinding
import com.demo.utils.ClientSocketListener
import com.demo.utils.ServerSocketListener
import com.demo.utils.SocketMessageListener
import com.demo.utils.SocketUtils
import java.net.ServerSocket

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var client: SocketUtils.Client
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initListener()
        initData()
    }

    private fun initData() {
        client = SocketUtils.Client()
        client.setClientSocketListener(object : ClientSocketListener {
            override fun onConnectSuccess() {
                runOnUiThread {
                    toast("创建成功")
                }
            }

            override fun onConnectFailure() {
                runOnUiThread {
                    toast("创建失败")

                }
            }
        })
        client.setMsgListener(object : SocketMessageListener {
            override fun onSentSuccess() {
                runOnUiThread {
                    toast("发送成功")

                }
            }

            override fun onSentFailure() {
                runOnUiThread {
                    toast("发送失败")

                }
            }

            override fun onReceived(key: String, msg: ByteArray) {
                runOnUiThread {
                    binding.tvMsg.append("\n")
                    binding.tvMsg.append("{key: $key, msg: ${String(msg, Charsets.UTF_8)}")
                }

            }

        })
    }

    private fun initListener() {
        binding.btnSet.setOnClickListener {
            val host = binding.etHost.text.toString()
            val port = binding.etPort.text.toString()
            try {
                if (TextUtils.isEmpty(host) || TextUtils.isEmpty(port)){
                    toast("请输入正确内容")
                    return@setOnClickListener
                }
                client.connectServer(host, port.toInt())
            }catch (e: Exception) {
                e.printStackTrace()
                toast("请输入正确内容")
                return@setOnClickListener
            }
        }

        binding.btnSend.setOnClickListener {
            client.sendMsgByAny("msg", binding.etSend.text.toString())
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        client.destroy()
        super.onDestroy()
    }
}