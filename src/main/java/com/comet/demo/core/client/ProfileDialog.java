package com.comet.demo.core.client;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

public class ProfileDialog extends Dialog<Pair<String, String>> {

    public ProfileDialog(String currentDisplayName, String currentImageUrl) {
        setTitle("Update Profile");
        setHeaderText("Update your profile information");

        // Set the button types
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        // Create the username and image URL labels and fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField displayNameField = new TextField(currentDisplayName);
        displayNameField.setPromptText("Display Name");
        TextField imageUrlField = new TextField(currentImageUrl);
        imageUrlField.setPromptText("Image URL");

        grid.add(new Label("Display Name:"), 0, 0);
        grid.add(displayNameField, 1, 0);
        grid.add(new Label("Image URL:"), 0, 1);
        grid.add(imageUrlField, 1, 1);

        getDialogPane().setContent(grid);

        // Convert the result to a display-name-image-url pair when the update button is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                return new Pair<>(displayNameField.getText(), imageUrlField.getText());
            }
            return null;
        });
    }
}
