package com.example.bludrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NameActivity extends AppCompatActivity {

    private EditText etName;
    private Button btnContinue;
    private ImageView imgPreview;
    private ImageView[] options;

    // avatar resources
    private final int[] avatarResIds = new int[]{
            R.drawable.avatar1,
            R.drawable.avatar2,
            R.drawable.avatar3,
            R.drawable.avatar4,
            R.drawable.avatar5,
            R.drawable.avatar6
    };

    private int selectedIndex = 0; // default first avatar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name);

        etName = findViewById(R.id.etName);
        btnContinue = findViewById(R.id.btnContinue);
        imgPreview = findViewById(R.id.imgAvatarPreview);

        options = new ImageView[]{
                findViewById(R.id.avatarOption1),
                findViewById(R.id.avatarOption2),
                findViewById(R.id.avatarOption3),
                findViewById(R.id.avatarOption4),
                findViewById(R.id.avatarOption5),
                findViewById(R.id.avatarOption6)
        };

        // default preview
        imgPreview.setImageResource(avatarResIds[selectedIndex]);
        highlightSelected(selectedIndex);

        // har avatar option pe click listener
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            options[i].setOnClickListener(v -> {
                selectedIndex = index;
                imgPreview.setImageResource(avatarResIds[index]);
                highlightSelected(index);
            });
        }

        btnContinue.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Enter your name");
                return;
            }

            SharedPreferences sp = getSharedPreferences("BluDrop", MODE_PRIVATE);
            sp.edit()
                    .putString("user_name", name)
                    .putInt("user_avatar_res", avatarResIds[selectedIndex])
                    .apply();

            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void highlightSelected(int index) {
        for (int i = 0; i < options.length; i++) {
            if (i == index) {
                options[i].setAlpha(1.0f);
            } else {
                options[i].setAlpha(0.4f);
            }
        }
    }
}
