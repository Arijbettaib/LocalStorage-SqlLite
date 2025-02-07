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

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText inputEditText;
    private EditText emailEditText;  // EditText for email
    private TextView outputTextView;
    private SharedPreferences sharedPreferences;
    private static final String PREF_KEY = "my_text";
    private AppDatabase db;
    private Handler mainThreadHandler; // Handler to run code on the main thread

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des vues
        inputEditText = findViewById(R.id.inputEditText);
        emailEditText = findViewById(R.id.emailEditText);  // Initialisation du champ email
        outputTextView = findViewById(R.id.outputTextView);
        Button saveButton = findViewById(R.id.saveButton);
        Button loadButton = findViewById(R.id.loadButton);
        Button addUserButton = findViewById(R.id.addUserButton);
        Button getUsersButton = findViewById(R.id.getUsersButton);
        Button getUserByIdButton = findViewById(R.id.getUserByIdButton);

        // Initialisation des SharedPreferences et de la base de données
        sharedPreferences = getPreferences(MODE_PRIVATE);
        db = AppDatabase.getDatabase(this);
        mainThreadHandler = new Handler(Looper.getMainLooper()); // Handler pour exécuter du code sur le thread principal

        // Gestion des clics sur les boutons
        loadButton.setOnClickListener(view -> loadData());
        saveButton.setOnClickListener(view -> saveData());

        addUserButton.setOnClickListener(view -> new Thread(this::addUser).start());
        getUsersButton.setOnClickListener(view -> new Thread(this::getAllUsers).start());
        getUserByIdButton.setOnClickListener(view -> new Thread(this::getUserById).start());
    }

    // Sauvegarde de données dans les SharedPreferences
    private void saveData() {
        String text = inputEditText.getText().toString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_KEY, text);
        editor.apply();
    }

    // Chargement de données depuis les SharedPreferences
    private void loadData() {
        String text = sharedPreferences.getString(PREF_KEY, "");
        outputTextView.setText(text);
    }

    // Ajout d'un utilisateur dans la base de données
    private void addUser() {
        String userName = inputEditText.getText().toString();
        String userEmail = emailEditText.getText().toString();  // Récupération de l'email saisi par l'utilisateur

        if (userName.isEmpty() || userEmail.isEmpty()) {
            // Affichage d'un message d'erreur si le nom ou l'email est vide
            mainThreadHandler.post(() -> Toast.makeText(MainActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show());
            return;
        }

        User user = new User(userName, userEmail);
        db.userDao().insert(user); // Opération de base de données

        // Affichage d'un message dans un Toast après l'ajout de l'utilisateur
        mainThreadHandler.post(() -> Toast.makeText(MainActivity.this, "Utilisateur ajouté (avec Thread)", Toast.LENGTH_SHORT).show());
    }

    // Récupération de tous les utilisateurs dans la base de données
    private void getAllUsers() {
        List<User> users = db.userDao().getAllUsers(); // Opération de base de données
        StringBuilder sb = new StringBuilder();
        for (User user : users) {
            sb.append("id: ").append(user.getId()).append(" name: ").append(user.getName()).append(" email: ").append(user.getEmail()).append("\n");
        }
        final String outputText = sb.toString();

        // Affichage du texte dans le TextView sur le thread principal
        mainThreadHandler.post(() -> outputTextView.setText(outputText));
    }

    // Récupération d'un utilisateur par son ID
    private void getUserById() {
        new Thread(() -> {
            String inputText = inputEditText.getText().toString();

            if (inputText.isEmpty()) {
                // Affichage d'un message Toast si l'ID est vide
                mainThreadHandler.post(() -> Toast.makeText(MainActivity.this, "Veuillez entrer un ID valide", Toast.LENGTH_SHORT).show());
                return;
            }

            try {
                int userId = Integer.parseInt(inputText); // Conversion de l'ID en entier

                User user = db.userDao().getUserByIdSync(userId); // Récupération de l'utilisateur synchroniquement

                // Mise à jour du TextView sur le thread principal
                mainThreadHandler.post(() -> {
                    if (user != null) {
                        outputTextView.setText("id: " + user.getId() + " name: " + user.getName() + " email: " + user.getEmail());
                    } else {
                        outputTextView.setText("Utilisateur non trouvé!");
                    }
                });
            } catch (NumberFormatException e) {
                // Si l'ID n'est pas un nombre valide, affichage d'un message d'erreur
                mainThreadHandler.post(() -> Toast.makeText(MainActivity.this, "ID invalide ! Veuillez entrer un nombre.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
