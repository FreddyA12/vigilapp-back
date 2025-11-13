# Guía de Prueba Local - Sistema de Alertas

## 📋 Pre-requisitos

- Java 21
- PostgreSQL con PostGIS
- Maven/Gradle
- Navegador moderno (Chrome, Firefox)
- Postman o cURL (opcional)
- Angular CLI (si usas Angular frontend)

## 🚀 1. Iniciar el Backend

```bash
# Clonar o navegar al proyecto
cd /path/to/vigilapp

# Limpiar y compilar
./gradlew clean build

# Ejecutar la aplicación (inicia automáticamente servicio Python)
./gradlew bootRun
```

**Esperado**:
```
✅ Liquibase migrations executed
✅ Face verification service started on port 8000
✅ Spring Boot started on port 8080
✅ Connection to database successful
```

## 🔐 2. Obtener JWT Token

### Opción A: Registrarse (si no existe usuario)
```bash
curl -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Juan",
    "lastName": "Pérez",
    "documentType": "cedula",
    "documentNumber": "1234567"
  }'
```

### Opción B: Login
```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Respuesta**:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "test@example.com",
    "firstName": "Juan",
    "lastName": "Pérez"
  }
}
```

**Guardar el token** para usarlo en siguientes requests:
```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9..."
```

## 📍 3. Configurar UserZone (Zona del Usuario)

```bash
curl -X POST http://localhost:8080/api/user-zones \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "centerLatitude": 10.3923,
    "centerLongitude": -75.4816,
    "radiusM": 5000
  }'
```

**Respuesta**: UserZoneDto con geometría creada

## 🔌 4. Conectar WebSocket (Frontend)

Abrir DevTools en navegador:

```javascript
// Script en consola del navegador
const userId = "550e8400-e29b-41d4-a716-446655440000"; // De paso 2

const socket = new WebSocket('ws://localhost:8080/ws/alerts');

socket.onopen = () => {
  console.log('✅ WebSocket conectado');

  // Registrar usuario
  socket.send(JSON.stringify({
    'command': 'SUBSCRIBE',
    'id': '1',
    'destination': '/app/alerts/register',
    'body': JSON.stringify({ userId: userId })
  }));
};

socket.onmessage = (event) => {
  console.log('📬 Mensaje recibido:', event.data);
  // Aquí mostrar notificación
};

socket.onerror = (error) => {
  console.error('❌ Error:', error);
};

socket.onclose = () => {
  console.log('🔌 Desconectado');
};
```

**Alternativa usando STOMP.js (más fácil)**:

```html
<!-- En HTML -->
<script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/dist/stomp.umd.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>

<script>
const userId = "550e8400-e29b-41d4-a716-446655440000";
const socket = new SockJS('http://localhost:8080/ws/alerts');
const stompClient = new StompJs.Client({
  webSocketFactory: () => socket,
  onConnect: (frame) => {
    console.log('✅ Conectado');

    // Registrar
    stompClient.publish({
      destination: '/app/alerts/register',
      body: JSON.stringify({ userId: userId })
    });

    // Suscribirse
    stompClient.subscribe(`/user/${userId}/topic/alerts`, (message) => {
      const alert = JSON.parse(message.body);
      console.log('🔔 Nueva alerta:', alert);
      console.log('Título:', alert.alertTitle);
      console.log('Categoría:', alert.alertCategory);
      console.log('Ubicación:', alert.latitude, alert.longitude);
    });
  }
});

stompClient.activate();
</script>
```

**Guardar el ID del usuario**:
```javascript
const USER_ID = "550e8400-e29b-41d4-a716-446655440000";
```

## 🚨 5. Crear Alerta (Triggers WebSocket)

### Opción A: cURL
```bash
curl -X POST http://localhost:8080/api/alerts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Incendio en progreso",
    "description": "Se reporta incendio en sector norte",
    "category": "EMERGENCY",
    "latitude": 10.3923,
    "longitude": -75.4816,
    "radiusM": 2000,
    "isAnonymous": false
  }'
```

### Opción B: Postman
1. POST a `http://localhost:8080/api/alerts`
2. Headers: `Authorization: Bearer {token}`
3. Body (JSON):
```json
{
  "title": "Incendio en progreso",
  "description": "Sector norte",
  "category": "EMERGENCY",
  "latitude": 10.3923,
  "longitude": -75.4816,
  "radiusM": 2000,
  "isAnonymous": false
}
```

**Resultado**:
1. En la consola del navegador (WebSocket): ✅ Recibirás mensaje en `/topic/alerts`
2. En respuesta HTTP: AlertDto creado

## 🗺️ 6. Probar Endpoints de Alertas

### Alertas Recientes
```bash
curl http://localhost:8080/api/alerts/recent?page=0&size=10 \
  -H "Authorization: Bearer $TOKEN"
```

### Búsqueda Avanzada
```bash
# Buscar alertas EMERGENCY activas creadas en últimos 7 días
curl "http://localhost:8080/api/alerts/search?category=EMERGENCY&status=ACTIVE&skip=0&limit=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Mapa de Calor
```bash
# Bounds alrededor de Cartagena
curl "http://localhost:8080/api/alerts/heatmap?swLat=10.3&swLon=-75.6&neLat=10.5&neLon=-75.4&gridSizeM=1000" \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta esperada**:
```json
[
  {
    "latitude": 10.395,
    "longitude": -75.48,
    "intensity": 3
  },
  {
    "latitude": 10.405,
    "longitude": -75.47,
    "intensity": 1
  }
]
```

### Estadísticas
```bash
curl "http://localhost:8080/api/alerts/stats?timeRange=7d" \
  -H "Authorization: Bearer $TOKEN"
```

## 📬 7. Probar Endpoints de Notificaciones

### Mis Notificaciones
```bash
curl http://localhost:8080/api/notifications/me?page=0&size=20 \
  -H "Authorization: Bearer $TOKEN"
```

### Contador de No Entregadas
```bash
curl http://localhost:8080/api/notifications/undelivered/count \
  -H "Authorization: Bearer $TOKEN"
```

### Marcar como Entregada
```bash
# Obtener ID de una notificación del paso anterior
curl -X PUT http://localhost:8080/api/notifications/{notificationId}/delivered \
  -H "Authorization: Bearer $TOKEN"
```

## 🧪 Escenario Completo de Prueba

### Paso 1: Abrir 2 navegadores
- **Navegador A**: Usuario creador de alerta
- **Navegador B**: Usuario receptor (otra zona)
- **Navegador C**: Usuario receptor (misma zona)

### Paso 2: Configurar usuarios
- A: Login con usuario 1, zona en (10.39, -75.48)
- B: Login con usuario 2, zona en (10.40, -75.50) ← Lejos
- C: Login con usuario 3, zona en (10.39, -75.48) ← Cerca

### Paso 3: Conectar todos a WebSocket
```javascript
// En cada navegador
const stompClient = new StompJs.Client({...});
stompClient.activate();
```

### Paso 4: Usuario A crea alerta en su zona
```bash
POST /api/alerts
{
  "title": "Incendio reportado",
  "latitude": 10.3923,
  "longitude": -75.4816,
  ...
}
```

### Paso 5: Verificar notificaciones
- **Navegador C** (misma zona): ✅ Recibe WebSocket notification
- **Navegador B** (otra zona): ❌ NO recibe notificación
- **Navegador A** (creador): ❌ NO recibe su propia alerta

---

## 🐛 Debugging

### Ver conexiones activas
```bash
curl http://localhost:8080/api/alerts/connected-users
# Respuesta: {"connectedUsers": 2}
```

### Logs del servidor
```
Terminal donde corre Spring Boot:

✅ Usuario conectado: 550e8400-e29b-41d4-a716-446655440000 (sesión: abc123)
Nueva alerta creada: Incendio en progreso
Enviando notificación a usuario: 550e8400-e29b-41d4-a716-446655440001
```

### Verificar en BD
```sql
-- Alertas creadas
SELECT id, title, status, created_at FROM alerts ORDER BY created_at DESC;

-- Notificaciones
SELECT id, status, created_at FROM notifications ORDER BY created_at DESC;

-- Zonas de usuario
SELECT id, user_id, ST_AsGeoJSON(geometry) FROM user_zones;
```

---

## ⚠️ Errores Comunes

### Error: "Usuario no tiene zona configurada"
**Solución**: Crear UserZone con `POST /api/user-zones`

### WebSocket desconecta rápido
**Causa**: Token expirado o CORS issue
**Solución**: Renovar token, verificar CORS en WebSocketConfig

### Alerta no llega por WebSocket
**Causa**: Usuario no está conectado o zona no intersecta
**Solución**:
1. Verificar en `/api/alerts/connected-users`
2. Verificar zona con `ST_Intersects` en BD

### Heatmap vacío
**Causa**: Sin alertas en bounds especificados
**Solución**: Crear alertas dentro del bounding box

---

## 📊 Checklist de Prueba

- [ ] Login y obtener JWT
- [ ] Crear UserZone
- [ ] Conectar WebSocket
- [ ] Crear alerta
- [ ] Recibir WebSocket notification
- [ ] Ver alertas recientes
- [ ] Buscar con filtros
- [ ] Ver heatmap
- [ ] Ver estadísticas
- [ ] Ver notificaciones guardadas
- [ ] Marcar notificación como entregada
- [ ] Desconectar WebSocket
- [ ] Verificar desconexión en servidor

---

## 🚀 Próximos Pasos

1. Integrar frontend (Angular/React)
2. Agregar UI para mapa con heatmap
3. Notificaciones push con FCM
4. Persistencia de WebSocket con Redis
5. Autenticación nativa WebSocket

---

## 📞 Soporte

**Si algo falla**:
1. Revisar logs de Spring Boot
2. Verificar PostgreSQL/PostGIS
3. Revisar CORS settings
4. Verificar JWT token válido
5. Revisar zona de usuario existe

**Documentación**:
- IMPLEMENTATION_SUMMARY.md
- WEBSOCKET_GUIDE.md
- CLAUDE.md (arquitectura general)
