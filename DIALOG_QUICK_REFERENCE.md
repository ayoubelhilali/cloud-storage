# Quick Reference: Custom Dialogs

## 🎯 Usage Examples

### 1. Add Folder Dialog

```java
// In DashboardController or any controller
@FXML
private void handleAddFolder() {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/cloudstorage/fx/AddFolderDialog.fxml")
        );
        Parent root = loader.load();
        
        AddFolderDialogController controller = loader.getController();
        
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
            getClass().getResource("/css/dialogs.css").toExternalForm()
        );
        
        dialogStage.setScene(scene);
        controller.setStage(dialogStage);
        controller.setOnSuccess(() -> {
            // Refresh your UI here
            loadUserFolders();
        });
        
        dialogStage.showAndWait();
        
    } catch (IOException e) {
        AlertUtils.showError("Error", "Could not open dialog.");
    }
}
```

### 2. Add to Folder Dialog

```java
// In FileRowFactory or any component
private static void addToFolder(Map<String, String> fileData, Runnable refreshCallback) {
    String fileName = fileData.get("name");
    
    try {
        FXMLLoader loader = new FXMLLoader(
            FileRowFactory.class.getResource("/com/cloudstorage/fx/AddToFolderDialog.fxml")
        );
        Parent root = loader.load();
        
        AddToFolderDialogController controller = loader.getController();
        controller.setFileName(fileName);
        
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
            FileRowFactory.class.getResource("/css/dialogs.css").toExternalForm()
        );
        
        dialogStage.setScene(scene);
        controller.setStage(dialogStage);
        controller.setOnSuccess(() -> {
            if (refreshCallback != null) {
                refreshCallback.run();
            }
        });
        
        dialogStage.showAndWait();
        
    } catch (IOException e) {
        AlertUtils.showError("Error", "Could not open dialog.");
    }
}
```

### 3. Confirmation Dialog (Updated in AlertUtils)

```java
// Using the enhanced AlertUtils
AlertUtils.showConfirmation(
    "Delete Account",
    "This will permanently delete your profile and all stored files. This action cannot be undone.",
    () -> {
        // This callback runs when user clicks "Delete"
        performDeletion();
    }
);
```

---

## 🎨 CSS Class Reference

### Dialog Container
```css
.custom-dialog              /* Main dialog container */
```

### Header Elements
```css
.dialog-icon               /* Icon with gradient color */
.dialog-title              /* Main title text */
.dialog-subtitle           /* Subtitle/description */
```

### Input Components
```css
.input-label               /* Label above inputs */
.dialog-input              /* Text field styling */
.dialog-input:focused      /* Focus state */
.dialog-combo              /* ComboBox styling */
```

### Feedback Elements
```css
.error-label               /* Error message text */
.info-box                  /* Information container */
.info-icon                 /* Info icon color */
.info-text                 /* Info text styling */
```

### Buttons
```css
.dialog-btn-cancel         /* Cancel/secondary button */
.dialog-btn-cancel:hover   /* Hover state */
.dialog-btn-cancel:pressed /* Pressed state */

.dialog-btn-primary        /* Primary action button */
.dialog-btn-primary:hover  /* Hover with glow */
.dialog-btn-primary:pressed /* Pressed state */
```

---

## 🔧 Controller Methods Reference

### AddFolderDialogController

```java
// Set the stage reference (required)
void setStage(Stage stage)

// Set callback for successful folder creation
void setOnSuccess(Runnable callback)

// Called automatically on "Create Folder" button
@FXML void handleCreate()

// Called automatically on "Cancel" button
@FXML void handleCancel()
```

### AddToFolderDialogController

```java
// Set the stage reference (required)
void setStage(Stage stage)

// Set the filename to be moved
void setFileName(String fileName)

// Set callback for successful file move
void setOnSuccess(Runnable callback)

// Called automatically on "Move File" button
@FXML void handleMove()

// Called automatically on "Cancel" button
@FXML void handleCancel()
```

---

## ✨ Animation System

Both dialogs use the same animation pattern:

```java
private void animateEntry() {
    // Fade animation (0 to 1 opacity)
    FadeTransition fade = new FadeTransition(Duration.millis(200), dialogRoot);
    fade.setFromValue(0);
    fade.setToValue(1);
    
    // Scale animation (0.9 to 1.0)
    ScaleTransition scale = new ScaleTransition(Duration.millis(200), dialogRoot);
    scale.setFromX(0.9);
    scale.setFromY(0.9);
    scale.setToX(1);
    scale.setToY(1);
    
    // Run both animations together
    ParallelTransition animation = new ParallelTransition(fade, scale);
    animation.play();
}
```

**Duration**: 200ms  
**Effects**: Fade + Scale (parallel)  
**Initial State**: Set in CSS (opacity: 0, scale: 0.9)

---

## 📋 Validation Rules

### Folder Name Validation
- ✅ Not empty
- ✅ Minimum 2 characters
- ✅ Maximum 50 characters
- ✅ Only alphanumeric, spaces, hyphens, and underscores
- ✅ Regex: `^[a-zA-Z0-9\\s\\-_]+$`

### Password Validation
- ✅ Not empty
- ✅ Minimum 6 characters
- ✅ Different from current password
- ✅ SHA-256 hashed before storage

---

## 🎯 Best Practices

1. **Always load CSS**: Include dialogs.css stylesheet
2. **Use TRANSPARENT stage**: For rounded corners to show
3. **Set MODAL**: Use APPLICATION_MODAL for blocking dialogs
4. **Provide callbacks**: Allow UI refresh after operations
5. **Handle errors**: Wrap in try-catch with user feedback
6. **Clear references**: Use showAndWait() for synchronous flow

---

## 🐛 Troubleshooting

### Dialog doesn't appear
- Check FXML path is correct
- Verify CSS file is loaded
- Ensure stage.show() or showAndWait() is called

### Animation doesn't work
- Verify dialogRoot is set correctly in FXML
- Check animateEntry() is called in setStage()
- Ensure CSS initial opacity/scale is set

### Styles not applying
- Check CSS file path in scene.getStylesheets()
- Verify styleClass names match CSS
- Use Scenic View to debug JavaFX node tree

### Validation not working
- Check TextField textProperty() listener is set up
- Verify regex pattern in validation
- Test with various input strings

---

## 📚 Related Files

**FXML**:
- `/frontend/src/main/resources/com/cloudstorage/fx/AddFolderDialog.fxml`
- `/frontend/src/main/resources/com/cloudstorage/fx/AddToFolderDialog.fxml`

**Controllers**:
- `/frontend/src/main/java/com/cloudstorage/fx/controllers/AddFolderDialogController.java`
- `/frontend/src/main/java/com/cloudstorage/fx/controllers/AddToFolderDialogController.java`

**Styles**:
- `/frontend/src/main/resources/css/dialogs.css`
- `/frontend/src/main/resources/css/alert.css` (for custom alerts)

**Utilities**:
- `/frontend/src/main/java/com/cloudstorage/fx/utils/AlertUtils.java`
- `/frontend/src/main/java/com/cloudstorage/database/FileDAO.java`

---

**Last Updated**: December 23, 2025  
**Version**: 1.0  
**Status**: Production Ready ✅

