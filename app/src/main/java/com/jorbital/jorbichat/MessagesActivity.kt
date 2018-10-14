package com.jorbital.jorbichat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_messages.*

class MessagesActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {
    companion object {

        private const val TAG = "MessagesActivity"

        //TODO: make this not a constant to support multiple conversations
        const val CONVERSATION_CHILD = "messages"

        private const val INVITE_RESULT = 1
        private const val IMAGE_RESULT = 2
        private const val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
        const val ANONYMOUS = "anonymous"
    }

    private var username: String? = ANONYMOUS
    private var photoUrl: String? = null
    private var googleApiClient: GoogleApiClient? = null

    private lateinit var linearLayoutManager: LinearLayoutManager

    // Firebase instance variables
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var firebaseUser: FirebaseUser? = null
    private var firebaseDatabaseReference: DatabaseReference? = null
    private var firebaseAdapter: MessagesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        // Set default username is anonymous.
        username = ANONYMOUS

        // Initialize Analytics for whole app
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            // Not signed in, launch the Login activity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        } else {
            username = firebaseUser?.displayName
            if (firebaseUser?.photoUrl != null) {
                photoUrl = firebaseUser?.photoUrl.toString()
            }
        }

        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build()

        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        messageRv.layoutManager = linearLayoutManager

        setUpMessagesList()

        setUpListeners()
    }

    public override fun onPause() {
        firebaseAdapter?.stopListening()
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        firebaseAdapter?.startListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.crash_menu -> {
                Log.w("Crashlytics", "Crash button clicked")
                causeCrash()
                return true
            }
            R.id.invite_menu -> {
                sendInvitation()
                return true
            }
            R.id.sign_out_menu -> {
                firebaseAuth.signOut()
                Auth.GoogleSignInApi.signOut(googleApiClient)
                username = ANONYMOUS
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setUpListeners() {
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                sendButton.isEnabled = charSequence.toString().trim { it <= ' ' }.isNotEmpty()
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        sendButton.setOnClickListener { sendMessage() }
        addMessageImageButton.setOnClickListener { addImage() }
    }

    private fun setUpMessagesList() {
        // New child entries
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        val parser = SnapshotParser { dataSnapshot ->
            val jorbichatMessage = dataSnapshot.getValue<JorbichatMessage>(JorbichatMessage::class.java)
            if (jorbichatMessage != null)
                jorbichatMessage.id = dataSnapshot.key
            jorbichatMessage!!
        }

        val messagesRef = firebaseDatabaseReference!!.child(CONVERSATION_CHILD)
        val options = FirebaseRecyclerOptions.Builder<JorbichatMessage>()
                .setQuery(messagesRef, parser)
                .build()

        firebaseAdapter = MessagesAdapter(options, progressBar)

        firebaseAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val messageCount = firebaseAdapter!!.itemCount
                val lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 || positionStart >= messageCount - 1 && lastVisiblePosition == positionStart - 1) {
                    messageRv.scrollToPosition(positionStart)
                }
            }
        })

        messageRv.adapter = firebaseAdapter
    }

    private fun sendMessage() {
        val jorbichatMessage = JorbichatMessage(messageEditText.text.toString(),
                username!!,
                photoUrl!!, null/* no image */)
        firebaseDatabaseReference!!.child(CONVERSATION_CHILD)
                .push().setValue(jorbichatMessage)
        messageEditText.setText("")
    }

    private fun addImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_RESULT)
    }

    private fun sendInvitation() {
        val intent = AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build()
        startActivityForResult(intent, INVITE_RESULT)
    }

    private fun causeCrash() {
        throw NullPointerException("Fake null pointer exception")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == IMAGE_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val uri = data.data
                    Log.d(TAG, "Uri: " + uri!!.toString())
                    val tempMessage = JorbichatMessage(null, username!!, photoUrl!!,
                            LOADING_IMAGE_URL)
                    firebaseDatabaseReference!!.child(CONVERSATION_CHILD).push()
                            .setValue(tempMessage) { databaseError, databaseReference ->
                                if (databaseError == null) {
                                    val key = databaseReference.key
                                    val storageReference = FirebaseStorage.getInstance()
                                            .getReference(firebaseUser!!.uid)
                                            .child(key!!)
                                            .child(uri.lastPathSegment!!)

                                    putImageInStorage(storageReference, uri, key)
                                } else {
                                    Log.w(TAG, "Unable to write message to database.",
                                            databaseError.toException())
                                }
                            }
                }
            }
        } else if (requestCode == INVITE_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                val payload = Bundle()
                payload.putString(FirebaseAnalytics.Param.VALUE, "sent")
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE,
                        payload)
                // Check how many invitations were sent and log.
                val ids = AppInviteInvitation.getInvitationIds(resultCode,
                        data!!)
                Log.d(TAG, "Invitations sent: " + ids.size)
            } else {
                val payload = Bundle()
                payload.putString(FirebaseAnalytics.Param.VALUE, "not sent")
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE,
                        payload)
                // Sending failed or it was canceled, show failure message to
                // the user
                Log.d(TAG, "Failed to send invitation.")
            }
        }
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri, key: String) {
        storageReference.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception!!
            }
            // Continue with the task to get the download URL
            storageReference.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val jorbichatMessage = JorbichatMessage(null, username!!, photoUrl!!,
                        task.result!!.toString())
                firebaseDatabaseReference!!.child(CONVERSATION_CHILD).child(key)
                        .setValue(jorbichatMessage)
            } else {
                Log.w(TAG, "Image upload task was not successful.",
                        task.exception)
            }
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:$connectionResult")
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show()
    }
}
