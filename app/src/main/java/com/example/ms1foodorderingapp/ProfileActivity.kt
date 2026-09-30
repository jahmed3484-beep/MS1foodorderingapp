package com.example.ms1foodorderingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ms1foodorderingapp.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        loadProfile()

        binding.saveButton.setOnClickListener {
            saveProfile()
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        binding.emailEditText.setText(user.email)

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.nameEditText.setText(
                    snapshot.child("name").getValue(String::class.java) ?: ""
                )
                binding.phoneEditText.setText(
                    snapshot.child("phone").getValue(String::class.java) ?: ""
                )
                binding.addressEditText.setText(
                    snapshot.child("address").getValue(String::class.java) ?: ""
                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfile() {
        val user = auth.currentUser ?: return

        val name = binding.nameEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val address = binding.addressEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill in all profile fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "address" to address
        )

        binding.saveButton.isEnabled = false

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(user.uid)
            .updateChildren(updates)
            .addOnCompleteListener { task ->
                binding.saveButton.isEnabled = true

                if (task.isSuccessful) {

                    if (password.isNotEmpty()) {
                        if (password.length < 6) {
                            Toast.makeText(
                                this,
                                "Password must be at least 6 characters",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addOnCompleteListener
                        }

                        user.updatePassword(password)
                            .addOnCompleteListener { passwordTask ->

                                if (passwordTask.isSuccessful) {
                                    binding.passwordEditText.text.clear()

                                    Toast.makeText(
                                        this,
                                        "Profile and password updated successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } else {
                                    Toast.makeText(
                                        this,
                                        "Profile updated, but password change failed: ${passwordTask.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                    } else {
                        Toast.makeText(
                            this,
                            "Profile updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    Toast.makeText(
                        this,
                        "Update failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}