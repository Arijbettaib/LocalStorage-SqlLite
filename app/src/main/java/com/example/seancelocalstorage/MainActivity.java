package com.example.seancelocalstorage;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText inputEditText, emailEditText;
    private TextView outputTextView;
    private SharedPreferences sharedPreferences;
    private static final String PREF_KEY = "my_text";
    private AppDatabase db;
    private Handler mainThreadHandler;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        emailEditText = findViewById(R.id.emailEditText);
        inputEditText = findViewById(R.id.inputEditText);
        outputTextView = findViewById(R.id.outputTextView);
        Button saveButton = findViewById(R.id.saveButton);
        Button loadButton = findViewById(R.id.loadButton);
        Button addUserButton = findViewById(R.id.addUserButton);
        Button getUsersButton = findViewById(R.id.getUsersButton);
        Button getUserByIdButton = findViewById(R.id.getUserByIdButton);

        sharedPreferences = getPreferences(MODE_PRIVATE);
        db = AppDatabase.getDatabase(this);
        mainThreadHandler = new Handler(Looper.getMainLooper());

        // RecyclerView et Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialiser l'adaptateur avec les utilisateurs actuels
        userAdapter = new UserAdapter(db.userDao().getAllUsers());
        recyclerView.setAdapter(userAdapter);

        // Ajouter des listeners pour les boutons
        saveButton.setOnClickListener(view -> saveData());
        loadButton.setOnClickListener(view -> loadData());
        addUserButton.setOnClickListener(view -> new Thread(this::addUser).start());
        getUsersButton.setOnClickListener(view -> new Thread(this::getAllUsers).start());
        getUserByIdButton.setOnClickListener(view -> new Thread(this::getUserById).start());
    }

    private void saveData() {
        String text = inputEditText.getText().toString();
        sharedPreferences.edit().putString(PREF_KEY, text).apply();
    }

    private void loadData() {
        outputTextView.setText(sharedPreferences.getString(PREF_KEY, ""));
    }

    private void addUser() {
        String userName = inputEditText.getText().toString();
        String email = emailEditText.getText().toString();

        if (userName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer le nom et l'email", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User(userName, email);
        db.userDao().insert(user);

        mainThreadHandler.post(() -> {
            Toast.makeText(MainActivity.this, "Utilisateur ajouté", Toast.LENGTH_SHORT).show();
            getAllUsers(); // Rafraîchir la liste des utilisateurs après l'ajout
        });
    }

    private void getAllUsers() {
        List<User> users = db.userDao().getAllUsers();
        if (users != null && !users.isEmpty()) {
            mainThreadHandler.post(() -> userAdapter.updateData(users));
        } else {
            mainThreadHandler.post(() -> Toast.makeText(MainActivity.this, "Aucun utilisateur trouvé", Toast.LENGTH_SHORT).show());
        }
    }

    private void getUserById() {
        int userId = Integer.parseInt(inputEditText.getText().toString());
        User user = db.userDao().getUserByIdSync(userId);
        mainThreadHandler.post(() -> outputTextView.setText(user != null ? "ID: " + user.getId() + " Name: " + user.getName() : "Utilisateur non trouvé !"));
    }
}
