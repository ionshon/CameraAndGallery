package com.inu.cameraandgallery1

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.inu.cameraandgallery1.databinding.ActivityMainBinding
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity() {

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private lateinit var getContent : ActivityResultLauncher<Intent>
    private lateinit var getContent2 : ActivityResultLauncher<Intent>

    val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    val STORAGE_PERMISSION = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    val FLAG_PERM_CAMERA = 98
    val FLAG_PERM_STORAGE = 99

    val FLAG_REQ_CAMERA = 101
    val FLAG_REQ_STORAGE = 102

    val FLAG_REQ_GALLERY = 103

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
// 1. 공용저장소 권한이 있는지 확인
        requirePermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), FLAG_PERM_STORAGE)

// 바뀐 registerForActivityResult API 구현방법
        getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->  // type : null
                    binding.imagePreview.setImageURI(uri)
                }
            }
        }

        getContent2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
            if (result.resultCode == RESULT_OK) {
                realUri?.let { uri ->  // null이 아닐 때만, type : vnd.android.cursor.dir/image
                    val bitmap = loadBitmap(uri)
                    binding.imagePreview.setImageBitmap(bitmap)
                    realUri = null
                }
            }
        }

            /*    when (result.resultCode) {
                    FLAG_REQ_CAMERA -> {
                        //              if (data?.extras?.get("data") != null) {  // 미리보기 이미지
                        //                  val bitmap = data?.extras?.get("data") as Bitmap
                        /*             binding.imagePreview.setImageBitmap(bitmap)
                                     val uri = binding.saveImageFile
                                     binding.imagePreview */
                        realUri?.let { uri ->  // null이 아닐 때만
                            val bitmap = loadBitmap(uri)
                            binding.imagePreview.setImageBitmap(bitmap)

                            realUri = null
                        }
                    }
                    FLAG_REQ_GALLERY -> {
                        result.data?.data?.let { uri ->
                            binding.imagePreview.setImageURI(uri)
                        }
                    } */


    }

    fun initViews() {
        // 2. 카메라 요청시 권한을 먼저 체크하고 승인되었으면 카메라 연다
        binding.buttonCamera.setOnClickListener {
            requirePermissions(arrayOf(Manifest.permission.CAMERA), FLAG_REQ_CAMERA)
        }
        //5. 갤러리 버튼이 클릭되면 갤러리 연다
        binding.buttonGallery.setOnClickListener {
            openGallery()
        }
    }

    // 원본 이미지의 주소를 저장할 변수
    var realUri: Uri? = null

    // 3. 카메라에 찍은 사진을 저장하기 위한 uri를 넘겨준다
    fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        createImageUri(newfileName(), "image/jpg")?.let { uri ->
            realUri = uri
            intent.putExtra(MediaStore.EXTRA_OUTPUT, realUri)
            if (intent.type == null)
                Log.d(TAG, "intent Type(카메라) : ${intent.type}") // null
            else Log.d(TAG, "intent Type(카메라) : 모름")  // vnd.android.cursor.dir/image
         //   startActivityForResult(intent, FLAG_REQ_CAMERA) 대체
            getContent2.launch(intent)
        }
    }

    fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        if (intent.type == "vnd.android.cursor.dir/image")
            Log.d(TAG, "intent Type(갤러리) : ${intent.type}")  // vnd.android.cursor.dir/image
        else Log.d(TAG, "intent Type(갤러리) : 모름")  // vnd.android.cursor.dir/image
       // startActivityForResult(intent, FLAG_REQ_GALLERY) 대체
        getContent.launch(intent)
    }

    // 원본 이미지를 저장할 uri를 미디어스토어(db)에 생성하는 메서드
    fun createImageUri(filename: String, mimeType: String) : Uri? {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)

        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    // 파일 이름을 생성하는 메서드
    fun newfileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val filename = sdf.format(System.currentTimeMillis())
        return "${filename}.jpg"
    }

    // 원본 이미지를 불러오는 메서드
    fun loadBitmap(photoUri:Uri) : Bitmap? {
        var image:Bitmap? = null
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                val source : ImageDecoder.Source = ImageDecoder.createSource(contentResolver, photoUri)
                image = ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
            }
        } catch (e:Exception) {
            e.printStackTrace()
        }
        return image

    }
    override fun permissionGranted(requestCode: Int) {
        when(requestCode) {
            FLAG_PERM_STORAGE -> initViews()
            FLAG_REQ_CAMERA -> openCamera()
        }
    }

    override fun permissionDenied(requestCode: Int) {
        when(requestCode) {
            FLAG_PERM_STORAGE -> {
                Toast.makeText(this, "공용 저장소 권한을 승인해야 앱을 사용 가능합니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
            FLAG_REQ_CAMERA -> {
                Toast.makeText(this, "카메라 권한을 승인해야 앱을 사용 가능합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setViews() {
        binding.buttonCamera.setOnClickListener {
            openCamera()
        }
    //    binding.buttonGallery.
    }

    // 4. 카메라를 찍은 후에 호출된다. 6. 갤러리에서 선택후 호출
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                FLAG_REQ_CAMERA -> {
      //              if (data?.extras?.get("data") != null) {  // 미리보기 이미지
      //                  val bitmap = data?.extras?.get("data") as Bitmap
           /*             binding.imagePreview.setImageBitmap(bitmap)
                        val uri = binding.saveImageFile
                        binding.imagePreview */
                        realUri?.let { uri ->  // null이 아닐 때만
                            val bitmap = loadBitmap(uri)
                            binding.imagePreview.setImageBitmap(bitmap)

                            realUri = null
                        }
                }
                FLAG_REQ_GALLERY -> {
                    data?.data?.let { uri ->
                        binding.imagePreview.setImageURI(uri)
                    }
                }
            }
        }
    }
}

    /*
    fun saveImageFile(filename: String, mimeType: String, bitmap: Bitmap) : Uri?{
        val values = ContentValues()
    }
*/
/*
    fun checkPermission(permissions: Array<out String>, flag: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, flag)
                    return false
                }
            }
        }
        return true
    }
*/
