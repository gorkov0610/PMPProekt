package uklo.fikt.pmp.pmpproekt.data

import android.content.Context
import android.util.Log
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import uklo.fikt.pmp.pmpproekt.R

@Suppress("DEPRECATION")
class AuthManager (private val context: Context){
    private val auth : FirebaseAuth = FirebaseAuth.getInstance()
    private val callbackManager = CallbackManager.Factory.create()
    private val db = FirebaseFirestore.getInstance()

    fun getCallbackManager() = callbackManager
    fun saveUserToFirestore() {
        val currentUser = auth.currentUser ?: return

        // Ако е анонимен, можеш да ставиш име "Gostin"
        val name = currentUser.displayName ?: (if (currentUser.isAnonymous) "Гостин" else "Корисник")

        val user = User(
            uid = currentUser.uid,
            name = name,
            email = currentUser.email ?: "",
            profilePicture = currentUser.photoUrl?.toString() ?: ""
        )

        db.collection("users").document(currentUser.uid)
            .set(user, SetOptions.merge()) // .merge() е важно за да не ги пребришеме постоечките полиња
            .addOnSuccessListener { Log.d("Firestore", "Корисникот е успешно зачуван!") }
            .addOnFailureListener { e -> Log.e("Firestore", "Грешка при зачувување", e) }
    }
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
    @Suppress("DEPRECATION")
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
    fun signInAnonymously(onResult: (Boolean) -> Unit) {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
    }
    fun signUpWithEmail(email: String, pass: String, onResult: (Boolean, Throwable?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception)
            }
    }

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, Throwable?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception)
            }
    }
    fun getCurrentUser() = auth.currentUser
    fun signOut() = auth.signOut()
    fun getErrorMessage(exception: Exception?, context: Context): String {
        if(exception == null) return context.getString(R.string.error_unknown)

        return when (exception) {
            // 1. Прво специфичните (под-класи)
            is FirebaseAuthWeakPasswordException -> context.getString(R.string.error_weak_password)
            is FirebaseAuthUserCollisionException -> context.getString(R.string.error_user_collision)
            is FirebaseAuthInvalidUserException -> context.getString(R.string.error_user_not_found)

            // 2. Потоа поопштата (parent class)
            is FirebaseAuthInvalidCredentialsException -> context.getString(R.string.error_wrong_password)

            // 3. Останати
            is FirebaseNetworkException -> context.getString(R.string.error_network)
            else -> context.getString(R.string.error_unknown)
        }
    }
}

