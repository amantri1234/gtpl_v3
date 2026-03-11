package com.gtplug.controllers;

import com.gtplug.config.AppConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public class PublicController {

    public void home(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("appName", AppConfig.APP_NAME);
        ctx.render("index.html", model);
    }

    public void manifest(Context ctx) {
        ctx.contentType("application/json");
        ctx.result("""
            {
              "name": "GTPLUG Underground Work Manager",
              "short_name": "GTPLUG",
              "description": "Manage underground cable installation work",
              "start_url": "/",
              "display": "standalone",
              "background_color": "#ffffff",
              "theme_color": "#0f172a",
              "orientation": "portrait",
              "icons": [
                {
                  "src": "/icons/icon-72x72.png",
                  "sizes": "72x72",
                  "type": "image/png"
                },
                {
                  "src": "/icons/icon-96x96.png",
                  "sizes": "96x96",
                  "type": "image/png"
                },
                {
                  "src": "/icons/icon-128x128.png",
                  "sizes": "128x128",
                  "type": "image/png"
                },
                {
                  "src": "/icons/icon-144x144.png",
                  "sizes": "144x144",
                  "type": "image/png"
                },
                {
                  "src": "/icons/icon-152x152.png",
                  "sizes": "152x152",
                  "type": "image/png"
                },
                {
                  "src": "/icons/icon-192x192.png",
                  "sizes": "192x192",
                  "type": "image/png"
                },
                {
                  "src": "/icons/icon-384x384.png",
                  "sizes": "384x384",
                  "type": "image/png"
                },
                {
                  "src": "/icons/icon-512x512.png",
                  "sizes": "512x512",
                  "type": "image/png"
                }
              ]
            }
            """);
    }

    public void serviceWorker(Context ctx) {
        ctx.contentType("application/javascript");
        ctx.result("""
            const CACHE_NAME = 'gtplug-v1';
            const urlsToCache = [
              '/',
              '/login',
              '/static/css/main.css',
              '/static/js/main.js',
              '/offline'
            ];
            
            self.addEventListener('install', event => {
              event.waitUntil(
                caches.open(CACHE_NAME)
                  .then(cache => cache.addAll(urlsToCache))
              );
              self.skipWaiting();
            });
            
            self.addEventListener('fetch', event => {
              event.respondWith(
                caches.match(event.request)
                  .then(response => {
                    if (response) {
                      return response;
                    }
                    return fetch(event.request)
                      .catch(() => {
                        if (event.request.mode === 'navigate') {
                          return caches.match('/offline');
                        }
                      });
                  })
              );
            });
            
            self.addEventListener('activate', event => {
              event.waitUntil(
                caches.keys().then(cacheNames => {
                  return Promise.all(
                    cacheNames.map(cacheName => {
                      if (cacheName !== CACHE_NAME) {
                        return caches.delete(cacheName);
                      }
                    })
                  );
                })
              );
              self.clients.claim();
            });
            """);
    }
}
