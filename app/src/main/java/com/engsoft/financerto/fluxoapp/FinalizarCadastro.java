package com.engsoft.financerto.fluxoapp;

import static com.engsoft.financerto.conexaofrontend.CadastroConexao.cadastrarUsuario;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.engsoft.financerto.R;
import com.engsoft.financerto.interfaces.CadastroCallback;

public class FinalizarCadastro extends AppCompatActivity {

    private EditText editSenha, editConfirmarSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_finalizar_cadastro);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getSupportActionBar().hide();

        editSenha = findViewById(R.id.edit_senha);
        editConfirmarSenha = findViewById(R.id.edit_confirmar_senha);

        Button btnCadastrar = findViewById(R.id.btn_cadastrar);
        btnCadastrar.setOnClickListener(view -> {

            String nome = getIntent().getStringExtra("nome");
            String sobreNome = getIntent().getStringExtra("sobreNome");
            String email = getIntent().getStringExtra("email");
            String senha = editSenha.getText().toString();
            String confirmarSenha = editConfirmarSenha.getText().toString();

            if (TextUtils.isEmpty(senha)) {
                editSenha.setError("Campo vazio!");
                return;
            }

            if (TextUtils.isEmpty(confirmarSenha)) {
                editConfirmarSenha.setError("Campo vazio!");
                return;
            }

            if (!senha.equals(confirmarSenha)) {
                editSenha.setError("As senhas não coincidem!");
                editConfirmarSenha.setError("As senhas não coincidem!");
                return;
            }

            if (!senhaValida(senha)) {
                editSenha.setError("A senha deve ter pelo menos 8 caracteres, incluindo uma letra maiúscula, um número e um caractere especial!");
                return;
            }

            Log.d("Cadastro", "Chamada ao método cadastrarUsuario");
            cadastrarUsuario(this, nome, sobreNome, email, senha, new CadastroCallback() {
                @Override
                public void onSuccess(String response) {
                    Intent intent = new Intent(FinalizarCadastro.this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(FinalizarCadastro.this, error, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }

    private boolean senhaValida(String senha){
        return senha.length() >= 8 &&
                senha.matches(".*[A-Z].*") &&
                senha.matches(".*\\d.*") &&
                senha.matches(".*[@#$%^&*+=!].*");
    }
}