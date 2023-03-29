package com.ad_passchanger.ad_passchanger;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXCheckListView;
import io.github.palexdev.materialfx.controls.MFXListView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    private Spinner<Integer> spinner;

    @FXML
    private MFXTextField exampleUser;

    @FXML
    private MFXTextField OU_Name;

    @FXML
    private MFXButton searchBtn;

    @FXML
    private MFXCheckListView<String> usersCheckList;

    ObservableList<String> users = FXCollections.observableArrayList();

    List<String> selectedUsers = FXCollections.observableArrayList();
    ObservableList<String> console = FXCollections.observableArrayList();

    @FXML
    private MFXListView<String> consoleList;

    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZZ";
    private static final Random RANDOM = new Random();

    @FXML
    void checkAll() {
        usersCheckList.getSelectionModel().selectItems(usersCheckList.getItems());
    }

    @FXML
    void unCheckAll() {
        usersCheckList.getSelectionModel().clearSelection();
    }

    void addToConsole(String text) {
        console.add(text);
        consoleList.setItems(console);
    }

    @FXML
    void clearUsers() {
        usersCheckList.getSelectionModel().clearSelection();
        users.clear();
    }

    @FXML
    void askForChangePasswords() {

        selectedUsers = usersCheckList.getSelectionModel().getSelectedValues();
        int countedUsers = selectedUsers.size();

        if (countedUsers == 0) {
            myAlert(Alert.AlertType.ERROR, "Wystąpił błąd", "Błąd z listą użytkowników", "Lista użytkowników jest pusta");
        } else {
            boolean confirmation = myConfirmation("Potwierdź operację", "Czy na pewno chcesz wykonać tą akcję?", "Operacja zmieni hasła zaznaczonym użytkownikom");
            if (confirmation) {

                Integer length = spinner.getValue();

                if (countedUsers == 0) {
                    myAlert(Alert.AlertType.ERROR, "Wystąpił błąd", "Brak wymaganych danych", "Nie zaznaczyłeś użytkowników");
                }

                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Zapisz plik z hasłami");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Pliki CSV", "*.csv"),
                        new FileChooser.ExtensionFilter("Wszystkie pliki", "*.*"));

                File file = fileChooser.showSaveDialog(new Stage());


                if (file != null) {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        for (String login : selectedUsers) {
                            String password = passwordGenerator(length);
                            writer.write(login + " " + password + "!@");
                            writer.newLine();
                        }
                    } catch (IOException e) {
                        myAlert(Alert.AlertType.ERROR, "Wystąpił błąd", "Błąd zapisu pliku", "Może masz uprawnień do zapisu w tej lokalizacji?");
                    }
                } else {
                    myAlert(Alert.AlertType.ERROR, "Wystąpił błąd", "Błąd zapisu pliku", "Nie wybrałeś pliku");
                }
                try {
                    assert file != null;
                    setPasswordsFromFile(file.getAbsolutePath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

    void setPasswordsFromFile(String fileName) throws IOException {

        boolean showedAlert = false;

        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\s+");
            String username = parts[0];
            String password = parts[1];

            String[] command = {"powershell.exe", "Set-ADAccountPassword -Identity " + username + " -NewPassword (ConvertTo-SecureString -AsPlainText " + password + " -Force) -Reset"};


            Process powerShellProcess = Runtime.getRuntime().exec(command);

            powerShellProcess.getOutputStream().close();

            String output = "";
            BufferedReader stdout = new BufferedReader(new InputStreamReader(powerShellProcess.getInputStream()));
            while ((line = stdout.readLine()) != null) {
                output += line + "\n";
            }

            String error = "";
            BufferedReader stderr = new BufferedReader(new InputStreamReader(powerShellProcess.getErrorStream()));
            while ((line = stderr.readLine()) != null) {
                error += line + "\n";
            }

            int exitCode;
            try {
                exitCode = powerShellProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for PowerShell command to complete", e);
            }

            if (exitCode != 0) {
                myAlert(Alert.AlertType.ERROR, +exitCode + "\n\nOutput:\n" + output, "Error:" + error, "");
                throw new IOException("PowerShell command exited with code " + exitCode + "\n\nOutput:\n" + output + "\nError:\n" + error);
            } else {
                if (!showedAlert) {
                    myAlert(Alert.AlertType.INFORMATION, "Sukces", "Operacja zakończyła się powodzeniem", "Hasła do kont zostały zresetowane");
                    showedAlert = true;
                }
            }
        }

        br.close();
    }


    private String passwordGenerator(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = RANDOM.nextInt(CHARSET.length());
            char randomChar = CHARSET.charAt(randomIndex);
            sb.append(randomChar);
        }
        return sb.toString();
    }

    @FXML
    void showUsers() {
        clearUsers();
        String OUName = OU_Name.getText();
        if (OUName.isEmpty()) {
            OU_Name.setStyle("-fx-border-color: tomato");
        } else {
            OU_Name.setStyle("-fx-border-color: white");
            String[] cmd = {"powershell.exe", "Get-ADUser -Filter * -SearchBase '" + OUName + "' | Select-Object SamAccountName"};
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = null;
            try {
                process = pb.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while (true) {
                try {
                    if ((line = reader.readLine()) == null) break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (!line.isEmpty()) {
                    if (!(line.contains("SamAccountName") || (line.contains("--------------")) || (line.contains("Get-ADUser")))) {
                        users.add(line);
                    }
                }
            }
            usersCheckList.setItems(users);
            checkAll();
        }
    }


    @FXML
    void getOU_DC() {
        String OU_User = exampleUser.getText();
        if (OU_User.isEmpty()) {
            exampleUser.setStyle("-fx-border-color: red");
        } else {
            exampleUser.setStyle("-fx-background-color: #a9a9a9 , white , white");

            String username = exampleUser.getText();
            try {
                ProcessBuilder build_test = new ProcessBuilder(
                        "cmd.exe", "/c", "dsquery user -samid " + username);
                Process p = build_test.start();
                int i = 0;

                BufferedReader output_reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String output;
                while ((output = output_reader.readLine()) != null) {
                    i++;
                    String out = output.replaceAll("CN=[^,]+,", "");
                    OU_Name.setText(out);
                }
                if (i == 0) {
                    myAlert(Alert.AlertType.ERROR, "Błąd", "Wystąpił błąd", "Brak takiego użytkownika w AD");
                    OU_Name.setText("Brak takiego użytkownika w AD");
                    OU_Name.setStyle("-fx-border-color: tomato");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    void myAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.initOwner(searchBtn.getScene().getWindow());
        alert.showAndWait();
    }

    boolean myConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.initOwner(searchBtn.getScene().getWindow());

        Optional<ButtonType> result = alert.showAndWait();
        return result.filter(buttonType -> buttonType == ButtonType.OK).isPresent();
    }

    @FXML
    void aboutProgram() {
        myAlert(Alert.AlertType.INFORMATION, "O programie", "AD_PassChanger służy do masowej zmiany haseł w AD",
                """
                        Umożliwia zmianę haseł dla grup użytkowników OU, sprecyzowanie długości hasła oraz wybranie konkretnych użytkowników

                        Autor: Wojciech Koziol
                        ©2023 Fujitsu Wszelkie prawa zastrzeżone""");
    }

    void setSpinner() {
        SpinnerValueFactory<Integer> valueFactory = //
                new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 25, 13);
        spinner.setValueFactory(valueFactory);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setSpinner();
    }
}
