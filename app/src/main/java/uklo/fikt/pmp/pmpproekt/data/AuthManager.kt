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
class AuthManager(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val callbackManager = CallbackManager.Factory.create()
    private val db = FirebaseFirestore.getInstance()

    fun getCallbackManager() = callbackManager
    fun getCurrentUser() = auth.currentUser
    fun signOut() = auth.signOut()

    fun saveUserToFirestore(customName: String? = null) { // 🛠️ Го тргаме customUsername
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val finalName = customName
            ?: currentUser.displayName
            ?: if (currentUser.isAnonymous) context.getString(R.string.chat_loading) else context.getString(R.string.user)

        val finalProfilePicture = if (currentUser.photoUrl != null) {
            currentUser.photoUrl.toString()
        } else if (currentUser.isAnonymous) {
            "https://api.dicebear.com/7.x/bottts/png?seed=$userId"
        } else {
            "https://ui-avatars.com/api/?name=${finalName}&background=A7F3D0&color=065F46&size=128"
        }

        val user = User(
            uid = userId,
            name = finalName,
            email = currentUser.email ?: "",
            profilePicture = finalProfilePicture
        )

        db.collection("users").document(userId)
            .set(user, SetOptions.merge())
            .addOnSuccessListener { Log.d("Firestore", "Корисникот [$finalName] е успешно зачуван!") }
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
                                saveUserToFirestore() // 🛠️ Автоматски зачувај во база
                                onResult(true, null)
                            } else {
                                onResult(false, task.exception?.message)
                            }
                        }
                }
                override fun onCancel() { onResult(false, "Canceled") }
                override fun onError(error: FacebookException) { onResult(false, error.message) }
            }
        )
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun signInWithGoogle(idToken: String, onResult: (Boolean) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserToFirestore() // 🛠️ Автоматски зачувај во база
                }
                onResult(task.isSuccessful)
            }
    }

    fun signInAnonymously(onResult: (Boolean) -> Unit) {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 🛠️ Автоматски зачувај како Гостин со DiceBear роботче
                    saveUserToFirestore(customName = "Гостин")
                }
                onResult(task.isSuccessful)
            }
    }

    fun signUpWithEmail(email: String, pass: String, name: String, onResult: (Boolean, Throwable?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 🛠️ Праќаме име и презиме за да генерира точни иницијали
                    saveUserToFirestore(customName = name)
                }
                onResult(task.isSuccessful, task.exception)
            }
    }

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, Throwable?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserToFirestore() // 🛠️ Се повикува за секој случај да ги освежи податоците
                }
                onResult(task.isSuccessful, task.exception)
            }
    }

    fun getErrorMessage(exception: Exception?, context: Context): String {
        if (exception == null) return context.getString(R.string.error_unknown)
        return when (exception) {
            is FirebaseAuthWeakPasswordException -> context.getString(R.string.error_weak_password)
            is FirebaseAuthUserCollisionException -> context.getString(R.string.error_user_collision)
            is FirebaseAuthInvalidUserException -> context.getString(R.string.error_user_not_found)
            is FirebaseAuthInvalidCredentialsException -> context.getString(R.string.error_wrong_password)
            is FirebaseNetworkException -> context.getString(R.string.error_network)
            else -> context.getString(R.string.error_unknown)
        }
    }
}