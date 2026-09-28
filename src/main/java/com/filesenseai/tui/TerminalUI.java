package com.filesenseai.tui;

import com.filesenseai.pipeline.ProgressListener;
import com.filesenseai.pipeline.SortResult;
import com.filesenseai.pipeline.SortingPipeline;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class TerminalUI {

    private final SortingPipeline sortingPipeline;

    public TerminalUI(SortingPipeline sortingPipeline) {
        this.sortingPipeline = sortingPipeline;
    }

    public void start() throws IOException {
        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();

        MultiWindowTextGUI textGUI = new MultiWindowTextGUI(screen);

        Path selectedFolder = showFolderBrowser(textGUI);

        if (selectedFolder != null && showConfirmation(textGUI, selectedFolder)) {
            boolean performBackup = showBackupPrompt(textGUI);
            runPipeline(textGUI, selectedFolder, performBackup);
        }

        screen.stopScreen();
    }

    private Path showFolderBrowser(MultiWindowTextGUI textGUI) {
        AtomicReference<Path> currentFolder = new AtomicReference<>(Paths.get(System.getProperty("user.home")));
        AtomicReference<Path> selectedFolder = new AtomicReference<>(null);

        BasicWindow window = new BasicWindow("FileSenseAI - Select a folder");
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        Label pathLabel = new Label(currentFolder.get().toString());
        panel.addComponent(pathLabel);

        ActionListBox listBox = new ActionListBox(new TerminalSize(60, 15));
        panel.addComponent(listBox);

        Runnable[] refreshHolder = new Runnable[1];
        refreshHolder[0] = () -> {
            listBox.clearItems();
            pathLabel.setText(currentFolder.get().toString());

            Path parent = currentFolder.get().getParent();
            if (parent != null) {
                listBox.addItem(".. (go up)", () -> {
                    currentFolder.set(parent);
                    refreshHolder[0].run();
                });
            }

            for (Path subFolder : listSubFolders(currentFolder.get())) {
                listBox.addItem(subFolder.getFileName().toString(), () -> {
                    currentFolder.set(subFolder);
                    refreshHolder[0].run();
                });
            }
        };

        refreshHolder[0].run();

        Button selectButton = new Button("Select This Folder", () -> {
            selectedFolder.set(currentFolder.get());
            window.close();
        });

        Button cancelButton = new Button("Cancel", window::close);

        Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        buttonPanel.addComponent(selectButton);
        buttonPanel.addComponent(cancelButton);
        panel.addComponent(buttonPanel);

        window.setComponent(panel);
        textGUI.addWindowAndWait(window);

        return selectedFolder.get();
    }

    private List<Path> listSubFolders(Path folder) {
        try (var paths = Files.list(folder)) {
            return paths.filter(Files::isDirectory).sorted().toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private boolean showConfirmation(MultiWindowTextGUI textGUI, Path folder) {
        int fileCount = countFiles(folder);
        String message = "Found " + fileCount + " files in " + folder
                + "\n\nFileSenseAI will read each file, group them by topic using AI, "
                + "and reorganize them into topic folders.\n\nContinue?";

        MessageDialogButton result = MessageDialog.showMessageDialog(
                textGUI, "Confirm", message, MessageDialogButton.Yes, MessageDialogButton.No);

        return result == MessageDialogButton.Yes;
    }

    private int countFiles(Path folder) {
        try (var paths = Files.list(folder)) {
            return (int) paths.filter(Files::isRegularFile).count();
        } catch (IOException exception) {
            return 0;
        }
    }

    private boolean showBackupPrompt(MultiWindowTextGUI textGUI) {
        MessageDialogButton result = MessageDialog.showMessageDialog(
                textGUI, "Backup",
                "Back up the original files to AWS S3 before reorganizing? This step is skipped automatically if AWS is not configured.",
                MessageDialogButton.Yes, MessageDialogButton.No);

        return result == MessageDialogButton.Yes;
    }

    private void runPipeline(MultiWindowTextGUI textGUI, Path folder, boolean performBackup) {
        BasicWindow progressWindow = new BasicWindow("Working");
        Label statusLabel = new Label("Starting...");
        Panel panel = new Panel();
        panel.addComponent(statusLabel);
        progressWindow.setComponent(panel);

        textGUI.addWindow(progressWindow);

        ProgressListener progressListener = message ->
                textGUI.getGUIThread().invokeLater(() -> statusLabel.setText(message));

        Thread worker = new Thread(() -> {
            try {
                SortResult result = sortingPipeline.run(folder, performBackup, progressListener);
                textGUI.getGUIThread().invokeLater(() -> {
                    progressWindow.close();
                    showSummary(textGUI, result);
                });
            } catch (Exception exception) {
                textGUI.getGUIThread().invokeLater(() -> {
                    progressWindow.close();
                    MessageDialog.showMessageDialog(textGUI, "Error",
                            "Something went wrong: " + exception.getMessage(), MessageDialogButton.OK);
                });
            }
        });

        worker.setDaemon(true);
        worker.start();

        try {
            while (progressWindow.isVisible()) {
                textGUI.processInput();
                textGUI.updateScreen();
                Thread.sleep(50);
            }
        } catch (Exception exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void showSummary(MultiWindowTextGUI textGUI, SortResult result) {
        StringBuilder message = new StringBuilder("Your files are now organized into these folders:\n\n");

        for (Map.Entry<String, List<Path>> entry : result.foldersToFiles().entrySet()) {
            message.append(entry.getKey()).append(" (").append(entry.getValue().size()).append(" files)\n");
        }

        if (result.backupLocation() != null) {
            message.append("\nOriginal files were backed up to S3 at: ").append(result.backupLocation());
        }

        MessageDialog.showMessageDialog(textGUI, "Done", message.toString(), MessageDialogButton.OK);
    }
}