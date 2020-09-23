package com.koala.messagebottle

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.koala.messagebottle.login.LoginActivity

private const val REQUEST_CODE_LOGIN = 100

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.main_fragment, container, false)

        val btnSignIn = rootView.findViewById<Button>(R.id.btnSignIn)
        val btnGetMessage = rootView.findViewById<Button>(R.id.btnGetMessage)
        val btnPostMessage = rootView.findViewById<Button>(R.id.btnPostMessage)

        btnSignIn.setOnClickListener {
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_LOGIN)
        }

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
