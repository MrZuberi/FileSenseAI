package com.filesenseai.web;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Component
public class BrowserLauncher {

    private volatile int port = 8080;

    @EventListener
    public void onWebServerInitialized(WebServerInitializedEvent event) {
        port = event.getWebServer().getPort();
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        String url = "http://localhost:" + port;

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception exception) {
            System.out.println("Could not open a browser automatically");
        }

        System.out.println("Open " + url + " in your browser");
    }
}