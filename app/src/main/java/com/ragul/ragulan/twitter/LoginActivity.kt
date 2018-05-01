package com.ragul.ragulan.twitter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_login.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

//import com.google.firebase.auth.FirebaseAuth


class LoginActivity : AppCompatActivity() {
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var PICK_IMAGE_CODE = 123
    private val READ_IMAGE = 222
    private var mAuth: FirebaseAuth? = null
    private var mStorageRef: StorageReference? = null
    private var database = FirebaseDatabase.getInstance()

    private var mRef = database.reference

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        imgvPerson.setOnClickListener(View.OnClickListener {
            checkPermission()

        })
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mAuth = FirebaseAuth.getInstance()
        mStorageRef = FirebaseStorage.getInstance().reference
    }


    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this , android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE) , READ_IMAGE)
                return
            }
        }
        loadImage()
    }


    override fun onRequestPermissionsResult(requestCode: Int , permissions: Array<out String> , grantResults: IntArray) {
        return when (requestCode) {
            READ_IMAGE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadImage()
                } else {
                    Toast.makeText(this , "Connect asses" , Toast.LENGTH_SHORT).show()
                }
            }
            else       -> {
                super.onRequestPermissionsResult(requestCode , permissions , grantResults)
            }
        }
    }

    private fun loadImage() {
        val intent = Intent(Intent.ACTION_PICK , android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent , PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int , resultCode: Int , data: Intent?) {
        super.onActivityResult(requestCode , resultCode , data)

        if (requestCode == PICK_IMAGE_CODE && data != null && resultCode == Activity.RESULT_OK) {

            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(selectedImage , filePathColumn , null , null , null)
            cursor.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)
            cursor.close()
            imgvPerson.setImageBitmap(BitmapFactory.decodeFile(picturePath))

        }
    }

    fun btnLoginClickEvent(view: View) {
        loginToFireBase(etEmail.text.toString() , etPassword.text.toString())

    }

    fun loginToFireBase(email: String , password: String) {
        mAuth!!.createUserWithEmailAndPassword(email , password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(applicationContext , "Successful Login" , Toast.LENGTH_SHORT).show()

                        saveImageInFireBase()

                    } else {
                        Toast.makeText(applicationContext , "Fail Login" , Toast.LENGTH_SHORT).show()
                    }
                }
    }


    private fun saveImageInFireBase() {

        val currentUser = mAuth!!.currentUser
        val storage = FirebaseStorage.getInstance()
        val email: String = splitString(currentUser.toString())
        val storageRef = storage.getReferenceFromUrl("gs://twitter-26ea5.appspot.com/")
        val df = SimpleDateFormat("ddMMyyHHmmss")
        val dataObject = Date()
        val imagePath = email.substring(1 , 4) + "." + df.format(dataObject) + ".jpg"
        var imageRef = storageRef.child("image/" + imagePath)

        imgvPerson.isDrawingCacheEnabled = true
        imgvPerson.buildDrawingCache()
        val drawable = imgvPerson.drawable as BitmapDrawable
        val bitMap = drawable.bitmap
        val baos = ByteArrayOutputStream()
        bitMap.compress(Bitmap.CompressFormat.JPEG , 100 , baos)
        val data = baos.toByteArray()
        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnFailureListener {
            Toast.makeText(applicationContext , "fail to Upload" , Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener { taskUpload ->
            var downloadURL = taskUpload.downloadUrl.toString()
            mRef.child("Users").child(currentUser!!.uid).child("email").setValue(currentUser.email)
            mRef.child("Users").child(currentUser!!.uid).child("ProfilImage").setValue(downloadURL)
            loadTweets()
            //Toast.makeText(applicationContext , "Successful upload" , Toast.LENGTH_SHORT).show()
        }
    }
    override fun onStart(){
        super.onStart()
        loadTweets()
    }

    fun loadTweets(){
        var currentUser=mAuth!!.currentUser
        if(currentUser!=null){
            var intent=Intent(this,MainActivity::class.java)
            intent.putExtra("email",currentUser.email)
            intent.putExtra("uid",currentUser.uid)
            startActivity(intent)

        }
    }

    fun splitString(email: String): String {
        var split = email.split("@")
        return split[0]
    }
}
