package uklo.fikt.pmp.pmpproekt.data

import android.content.Context
import android.content.Intent
import com.facebook.CallbackManager
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import uklo.fikt.pmp.pmpproekt.R

class AuthManager (private val context: Context){
    private val auth : FirebaseAuth = FirebaseAuth.getInstance()
    private val callbackManager = CallbackManager.Factory.create()

    fun getCallbackManager() = callbackManager

    fun handleFacebookLogin(onResult: (Boolean, String?) -> Unit) {
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onResult(true, null)
                            } else {
                                onResult(false, task.exception?.message)
                            }
                        }
                }

                override fun onCancel() {
                    onResult(false, "Canceled")
                }

                override fun onError(error: FacebookException) {
                    onResult(false, error.message)
                }
            }
        )
    }
    fun getGoogleSignInClient() : GoogleSignInClient{
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun signInWithGoogle(idToken : String, onResult: (Boolean) -> (Unit)){
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
    }

    fun getCurrentUser() = auth.currentUser
    fun signOut() = auth.signOut()
}

