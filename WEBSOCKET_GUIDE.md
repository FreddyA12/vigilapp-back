# WebSocket Guide - Notificaciones en Tiempo Real

## Descripción

El sistema de alertas usa WebSocket para enviar notificaciones en tiempo real a usuarios conectados. Cuando se crea una alerta, todos los usuarios cuya zona geográfica contiene la alerta reciben una notificación instantánea en la app.

## Tecnología

- **WebSocket Endpoint**: `ws://localhost:8080/ws/alerts`
- **Protocol**: STOMP (Simple Text Oriented Messaging Protocol) con SockJS
- **Message Broker**: Simple Message Broker (en memoria)

## Cómo Funciona

### 1. Usuario se conecta a la app (Login)

**Frontend:**
```typescript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export class AlertWebSocketService {
  private stompClient: Client;

  connect(userId: string) {
    const socket = new SockJS('http://localhost:8080/ws/alerts');
    this.stompClient = new Client({
      webSocketFactory: () => socket,
      onConnect: () => {
        console.log('Conectado al WebSocket');

        // Registrar el usuario
        this.stompClient.publish({
          destination: '/app/alerts/register',
          body: JSON.stringify({ userId: userId })
        });

        // Suscribirse a notificaciones personales
        this.stompClient.subscribe(`/user/${userId}/topic/alerts`, (message) => {
          const notification = JSON.parse(message.body);
          this.handleNewAlert(notification);
        });
      },
      onDisconnect: () => {
        console.log('Desconectado del WebSocket');
      }
    });

    this.stompClient.activate();
  }

  disconnect(userId: string) {
    // Desregistrar usuario
    this.stompClient.publish({
      destination: '/app/alerts/unregister',
      body: JSON.stringify({ userId: userId })
    });

    this.stompClient.deactivate();
  }

  private handleNewAlert(notification: AlertNotificationMessage) {
    console.log('Nueva alerta recibida:', notification);
    // Mostrar toast, notificación del navegador, actualizar mapa, etc.
    this.showNotification(notification);
  }

  private showNotification(alert: AlertNotificationMessage) {
    // Ejemplo con toast
    const message = `${alert.alertCategory}: ${alert.alertTitle}`;
    // Toast.show(message, 'info');

    // O notificación del navegador
    if ('Notification' in window) {
      new Notification('VigilApp - Nueva Alerta', {
        body: message,
        icon: '/assets/alert-icon.png'
      });
    }
  }
}
```

### 2. Se crea una nueva alerta

**Backend (Automático):**
```
POST /api/alerts
├─ Guardar alerta en DB
├─ Buscar usuarios cuya zona contiene la alerta
├─ Crear registros en tabla notifications
├─ Enviar mensaje WebSocket a usuarios conectados
└─ Retornar AlertDto
```

### 3. Usuario recibe notificación en tiempo real

**Estructura del Mensaje:**
```json
{
  "event": "NEW_ALERT",
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "alertTitle": "Incendio en progreso",
  "alertCategory": "EMERGENCY",
  "alertDescription": "Incendio reportado en sector norte",
  "latitude": 10.3923,
  "longitude": -75.4816,
  "createdByUserName": "Juan Pérez",
  "timestamp": 1698742800000
}
```

## Instalación Frontend

### 1. Instalar dependencias
```bash
npm install @stomp/stompjs sockjs-client
```

### 2. Crear servicio de WebSocket

```typescript
// src/app/services/alert-websocket.service.ts
import { Injectable } from '@angular/core';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

@Injectable({
  providedIn: 'root'
})
export class AlertWebSocketService {
  private stompClient: Client;
  private isConnected = false;

  constructor() {}

  connect(userId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const socket = new SockJS('http://localhost:8080/ws/alerts');

      this.stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('✅ Conectado al servidor de alertas');
          this.isConnected = true;

          // Registrar usuario
          this.stompClient.publish({
            destination: '/app/alerts/register',
            body: JSON.stringify({ userId: userId })
          });

          // Suscribirse a canal personal
          this.stompClient.subscribe(`/user/${userId}/topic/alerts`, (message) => {
            try {
              const notification = JSON.parse(message.body);
              this.onNewAlert(notification);
            } catch (e) {
              console.error('Error parsing message:', e);
            }
          });

          resolve();
        },
        onStompError: (frame) => {
          console.error('❌ Error STOMP:', frame);
          reject(frame);
        },
        onDisconnect: () => {
          console.log('🔌 Desconectado del servidor');
          this.isConnected = false;
        }
      });

      this.stompClient.activate();
    });
  }

  disconnect(userId: string): void {
    if (this.stompClient && this.isConnected) {
      // Notificar al servidor
      this.stompClient.publish({
        destination: '/app/alerts/unregister',
        body: JSON.stringify({ userId: userId })
      });

      this.stompClient.deactivate();
    }
  }

  private onNewAlert(notification: any): void {
    console.log('🔔 Nueva alerta:', notification);

    // Emitir evento para que componentes se suscriban
    // this.newAlertSubject.next(notification);

    // Mostrar notificación del navegador
    this.showBrowserNotification(notification);

    // Reproducir sonido (opcional)
    this.playSound();
  }

  private showBrowserNotification(alert: any): void {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(`${alert.alertCategory} - ${alert.alertTitle}`, {
        body: alert.alertDescription,
        icon: '/assets/icons/alert.png',
        tag: 'alert-' + alert.alertId,
        requireInteraction: true
      });
    }
  }

  private playSound(): void {
    // Reproducir sonido de notificación
    const audio = new Audio('/assets/sounds/notification.mp3');
    audio.play().catch(e => console.warn('No audio:', e));
  }

  isWebSocketConnected(): boolean {
    return this.isConnected;
  }
}
```

### 3. Usar en componente

```typescript
// app.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { AuthService } from './services/auth.service';
import { AlertWebSocketService } from './services/alert-websocket.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit, OnDestroy {
  constructor(
    private authService: AuthService,
    private websocketService: AlertWebSocketService
  ) {}

  ngOnInit() {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.websocketService.connect(user.id).then(() => {
        console.log('WebSocket conectado');
      }).catch(err => {
        console.error('Error conectando WebSocket:', err);
      });
    }
  }

  ngOnDestroy() {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.websocketService.disconnect(user.id);
    }
  }
}
```

### 4. Solicitar permiso para notificaciones

```typescript
// app.component.ts (en ngOnInit)
if ('Notification' in window && Notification.permission === 'default') {
  Notification.requestPermission();
}
```

## Endpoints REST Complementarios

### Ver notificaciones guardadas
```bash
GET /api/notifications/me?page=0&size=20
Authorization: Bearer {token}
```

### Marcar notificación como entregada
```bash
PUT /api/notifications/{notificationId}/delivered
Authorization: Bearer {token}
```

### Ver usuarios conectados (debug)
```bash
GET /api/alerts/connected-users
```

## Flujo Completo de Usuario

```
1. Usuario abre app
   ↓
2. Frontend: AuthService obtiene usuario
   ↓
3. Frontend: AlertWebSocketService.connect(userId)
   ↓
4. WebSocket conectado (/ws/alerts)
   ↓
5. Frontend publica: /app/alerts/register
   ↓
6. Backend: AlertNotificationService.registerUser(userId, sessionId)
   ↓
7. Frontend se suscribe a: /user/{userId}/topic/alerts
   ↓
8. [En cualquier momento] Otro usuario crea alerta
   ↓
9. Backend: checkea si alerta está en zona del usuario
   ↓
10. Si SÍ: Envía mensaje a /user/{userId}/topic/alerts
   ↓
11. Frontend recibe mensaje
   ↓
12. Mostrar toast/notificación/sonido/actualizar UI
```

## Debugging

### Verificar conexión
```typescript
console.log(this.websocketService.isWebSocketConnected());
```

### Ver usuarios conectados
```bash
curl http://localhost:8080/api/alerts/connected-users
# Respuesta: {"connectedUsers": 3}
```

### Logs del servidor
```
Usuario conectado: 550e8400-e29b-41d4-a716-446655440000 (sesión: abc123)
Nueva alerta creada: Incendio en progreso
Enviando notificación a usuario: 550e8400-e29b-41d4-a716-446655440000
```

## Notas Importantes

1. **Zona Requerida**: Usuario DEBE tener `UserZone` configurada para recibir notificaciones
2. **Solo Conectados**: Solo usuarios conectados reciben notificaciones en tiempo real
3. **Historial**: Las notificaciones se guardan en tabla `notifications` para consulta posterior
4. **Sin Creador**: Usuario que crea alerta NO recibe notificación de su propia alerta
5. **CORS**: Configurado para localhost:4200 y localhost:3000

## Estructura de Mensaje

```java
AlertNotificationMessage {
  - event: "NEW_ALERT"
  - alertId: UUID
  - alertTitle: String
  - alertCategory: String (EMERGENCY, PRECAUTION, INFO, COMMUNITY)
  - alertDescription: String
  - latitude: Double
  - longitude: Double
  - createdByUserName: String (null si anónima)
  - timestamp: Long (milliseconds)
}
```

## Limitaciones Actuales (MVP)

- ✅ Solo notificaciones en la app (no email/SMS)
- ✅ Solo filtrado por zona (no por categoría/ciudad aún)
- ✅ Mensages en memoria (no persistentes entre reinicios)
- ✅ Sin autenticación en WebSocket (usa JWT en HTTP primero)

## Próximas Mejoras

- [ ] Filtrado por categoría de alerta
- [ ] Notificaciones push via FCM
- [ ] Persistencia de conexiones con Redis
- [ ] Autenticación nativa de WebSocket
- [ ] Notificaciones batched para reducir overhead
