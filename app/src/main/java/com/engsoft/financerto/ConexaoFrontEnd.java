package com.engsoft.financerto;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConexaoFrontEnd {

    // URLS da sua API
    private static final String BASE_URL_CADASTRO = "https://finan-certo-api.onrender.com/usuarios/";
    private static final String BASE_URL_LOGIN = "https://finan-certo-api.onrender.com/api/gettoken/";
    private static final String BASE_URL_REFRESH = "https://finan-certo-api.onrender.com/api/token/refresh/";
    private static final String BASE_URL_FINANCAS_USUARIO = "https://finan-certo-api.onrender.com/financasusuario/";

    // Método genérico para configurar a conexão HTTP
    private static HttpURLConnection setupConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setDoOutput(true);
        return conn;
    }

    public static boolean isTokenExpired(String token) {
        try {
            String[] split = token.split("\\.");
            String payload = new String(Base64.decode(split[1], Base64.URL_SAFE));
            JSONObject json = new JSONObject(payload);
            long exp = json.getLong("exp");
            long currentTime = System.currentTimeMillis() / 1000;
            return currentTime >= exp;
        } catch (Exception e) {
            return true;
        }
    }
    public static void renovarAccessToken(Context context, TokenCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String refresh = prefs.getString("refresh_token", null);

        if (refresh == null) {
            callback.onError("Refresh token não encontrado.");
            return;
        }

        new Thread(() -> {
            try {
                HttpURLConnection conn = setupConnection(BASE_URL_REFRESH);

                JSONObject json = new JSONObject();
                json.put("refresh", refresh);

                sendRequest(conn, json.toString());

                int responseCode = conn.getResponseCode();
                String response = getResponse(conn);

                if (responseCode == 200) {
                    JSONObject jsonResponse = new JSONObject(response);
                    String newAccessToken = jsonResponse.optString("access", null);
                    if (newAccessToken != null) {
                        salvarTokens(context, newAccessToken, refresh); // Atualiza o access
                        callback.onSuccess(newAccessToken);
                    } else {
                        callback.onError("Novo access token não retornado.");
                    }
                } else {
                    callback.onError("Erro ao renovar token.");
                }
            } catch (Exception e) {
                callback.onError("Erro de conexão: " + e.getMessage());
            }
        }).start();
    }


    private static void salvarTokens(Context context, String access, String refresh){
        SharedPreferences prefs = context.getSharedPreferences("auth_prefes", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("access", access);
        editor.putString("refresh", refresh);
        editor.apply();
    }

    // Envia o corpo JSON para a conexão
    private static void sendRequest(HttpURLConnection conn, String jsonInput) throws IOException {
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.writeBytes(jsonInput);
            wr.flush();
        }
    }

    // Lê a resposta do servidor
    private static String getResponse(HttpURLConnection conn) throws IOException {
        InputStream inputStream = (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    // Método para cadastrar um usuário
    public static void cadastrarUsuario(String nome, String sobreNome, String email, String senha) {
        Log.d("Cadastro", "Método cadastrarUsuario foi chamado");

        new Thread(() -> {
            try {
                HttpURLConnection conn = setupConnection(BASE_URL_CADASTRO);

                JSONObject json = new JSONObject();
                json.put("first_name", nome);
                json.put("last_name", sobreNome);
                json.put("email", email);
                json.put("password", senha);

                sendRequest(conn, json.toString());

                int responseCode = conn.getResponseCode();
                String response = getResponse(conn);

                Log.d("Cadastro", "Response Code: " + responseCode);
                Log.d("Cadastro", "Resposta do servidor: " + response);

                // Você pode implementar um callback se quiser tratar isso na tela

            } catch (Exception e) {
                Log.e("Cadastro", "Erro na conexão: ", e);
            }
        }).start();
    }

    // Interface para tratar resultado do login
    public interface LoginCallback {
        void onSuccess(String token); // Token JWT
        void onError(String error);   // Mensagem de erro
    }

    public interface TokenCallback{
        void onSuccess(String newAccessToken);
        void onError(String error);
    }

    // Método de login com JWT
    public static void loginUsuario(Context context, String email, String senha, LoginCallback callback) {
        Log.d("Login", "Entrou no método loginUsuario!");

        new Thread(() -> {
            try {
                HttpURLConnection conn = setupConnection(BASE_URL_LOGIN);

                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", senha);

                sendRequest(conn, json.toString());

                int responseCode = conn.getResponseCode();
                String response = getResponse(conn);

                Log.d("Login", "Response Code: " + responseCode);
                Log.d("Login", "Resposta do servidor: " + response);

                if (responseCode == 200) {
                    JSONObject jsonResponse = new JSONObject(response);
                    String token = jsonResponse.optString("access", null);
                    String refreshToken = jsonResponse.optString("refresh", null);

                    if (token != null && refreshToken != null) {
                        salvarTokens(context, token, refreshToken);
                        callback.onSuccess(token);
                    } else {
                        callback.onError("Token não encontrado na resposta.");
                    }
                } else {
                    JSONObject jsonResponse = new JSONObject(response);
                    String errorMessage = jsonResponse.optString("detail", "Erro desconhecido!");
                    callback.onError(errorMessage);
                }

            } catch (Exception e) {
                Log.e("Login", "Erro na conexão: ", e);
                callback.onError("Erro de conexão: " + e.getMessage());
            }
        }).start();
    }

    public static void atualizarFinancasUsuario(Context context, int anoFinancas, int mesFinancas, double receitas, double despesas, double balancoFinancas){

        Log.d("Finanças", "Entrando no método de finanças.");

        new Thread(() -> {
            try{
                URL url = new URL(BASE_URL_FINANCAS_USUARIO);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("Content-Type", "application/json");

                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("FINANCAS_ANO", anoFinancas);
                json.put("MES_ATUAL", mesFinancas);
                json.put("FINANCAS_RECEITAS", receitas);
                json.put("FINANCAS_DESPESAS", despesas);
                json.put("FINANCAS_BALANCO", balancoFinancas);

                sendRequest(conn, json.toString());

                int responseCode = conn.getResponseCode();
                String response = getResponse(conn); // Use sua função para ler o InputStream

                Log.d("Finanças", "Response PUT: " + responseCode + " | " + response);

            }catch (Exception e){
                Log.e("Finanças", "Erro na conexão de finanças: ", e);
            }
        }).start();
    }

    public static void buscarFinancasUsuario(Context context, FinancasCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL_FINANCAS_USUARIO);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");

                String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .getString("access", null);
                if (token != null) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int responseCode = conn.getResponseCode();
                String response = getResponse(conn);

                Log.d("Finanças", "Response GET: " + responseCode + " | " + response);

                if (responseCode == 200) {
                    JSONObject jsonResponse = new JSONObject(response);
                    int ano = jsonResponse.optInt("FINANCAS_ANO");
                    int mes = jsonResponse.optInt("MES_ATUAL");
                    double receitas = jsonResponse.optDouble("FINANCAS_RECEITAS");
                    double despesas = jsonResponse.optDouble("FINANCAS_DESPESAS");
                    double balanco = jsonResponse.optDouble("FINANCAS_BALANCO");

                    callback.onSuccess(ano, mes, receitas, despesas, balanco);
                } else {
                    callback.onError("Erro ao buscar dados financeiros.");
                }

            } catch (Exception e) {
                Log.e("Finanças", "Erro na conexão de finanças (GET): ", e);
                callback.onError("Erro de conexão: " + e.getMessage());
            }
        }).start();
    }
    public interface FinancasCallback {
        void onSuccess(int ano, int mes, double receitas, double despesas, double balanco);
        void onError(String error);
    }

}
