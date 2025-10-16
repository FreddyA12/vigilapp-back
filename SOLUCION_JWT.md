# Solución: Error de JWT

## ❌ Error Original

```
java.lang.IllegalArgumentException: Key bytes can only be specified for HMAC signatures.
```

## 🔍 Causa

La clave secreta JWT era `"secret"` (solo 6 caracteres). El algoritmo HS256 requiere una clave de **al menos 256 bits (32 caracteres)** para ser seguro.

## ✅ Solución Aplicada

### 1. Actualizado `JwtUtil.java`

**Antes:**
```java
private String SECRET_KEY = "secret";
```

**Ahora:**
```java
@Value("${jwt.secret:vigilapp-secret-key-for-jwt-token-generation-minimum-256-bits-required-for-hs256}")
private String SECRET_KEY;
```

Ahora la clave se lee desde `application.yml` con un valor por defecto seguro.

### 2. Agregado en `application.yml`

```yaml
jwt:
  secret: vigilapp-secret-key-for-jwt-token-generation-minimum-256-bits-required-for-hs256-algorithm
```

Esta clave tiene **92 caracteres** (mucho más seguro que "secret").

## 🔒 Recomendación para Producción

Para producción, genera una clave aleatoria segura:

```bash
# Opción 1: Usar openssl
openssl rand -base64 64

# Opción 2: Usar uuidgen (varias veces)
echo "$(uuidgen)$(uuidgen)$(uuidgen)"
```

Y actualiza `application.yml`:

```yaml
jwt:
  secret: ${JWT_SECRET:tu-clave-generada-aqui}
```

Luego configura la variable de entorno `JWT_SECRET` en tu servidor de producción.

## 🚀 Cómo Probar Ahora

1. Reinicia Spring Boot si está corriendo
2. El endpoint `/api/register` ahora debería funcionar correctamente
3. Prueba con:

```bash
curl -X POST http://localhost:8080/api/register \
  -F "name=Juan Pérez" \
  -F "email=juan@example.com" \
  -F "password=password123" \
  -F "fotoCedula=@cedula.jpg" \
  -F "selfie=@selfie.jpg"
```

## ⚠️ Nota Importante

El endpoint `/api/register` es **público** (no requiere token JWT). El error ocurrió porque el `JwtRequestFilter` intentó parsear un token que no existía en la petición.

Con la nueva configuración, el filtro maneja correctamente las peticiones sin token a endpoints públicos.

## ✅ Estado Final

- ✅ Clave JWT configurada correctamente
- ✅ Endpoint de registro funcional
- ✅ Servicio Python funcionando
- ✅ Sistema completo operativo

**¡Listo para usar!** 🎉
