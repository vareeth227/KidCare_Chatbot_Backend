# KidCare Chatbot — Configuración de IA API

## Estado actual: OpenRouter (compatible con múltiples modelos)

El sistema fue migrado de Anthropic Claude directo a **OpenRouter**, un proxy que permite
usar múltiples modelos de IA de forma económica con el mismo código.

---

## Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `src/main/resources/application.properties` | Añadidas propiedades `claude.api.url` y `claude.api.auth-type` |
| `service/AnonymizationService.java` | URL y auth configurables via `@Value`; parser dual (OpenAI + Anthropic) |
| `controller/ChatController.java` | Mismo patrón que AnonymizationService |

---

## Configuración actual (OpenRouter)

En `application.properties`:

```properties
claude.api.url=${CLAUDE_API_URL:https://openrouter.ai/api/v1/chat/completions}
claude.api.key=${CLAUDE_API_KEY:}          # <- tu key de openrouter.ai/keys
claude.api.auth-type=${CLAUDE_API_AUTH_TYPE:bearer}
claude.model=${CLAUDE_MODEL:google/gemini-flash-1.5}
```

### Obtener tu API Key de OpenRouter
1. Ir a **https://openrouter.ai/keys**
2. Crear cuenta (gratis) → **Create Key**
3. Pegar la key en `claude.api.key=sk-or-v1-...`

### Modelos económicos disponibles en OpenRouter

| Modelo | Precio | Notas |
|--------|--------|-------|
| `google/gemini-flash-1.5` | ~$0.075/1M tokens | **Recomendado** — rápido y barato |
| `meta-llama/llama-3-8b-instruct` | Gratis (con límite) | Buena calidad, sin costo |
| `mistralai/mistral-7b-instruct` | ~$0.06/1M tokens | Ligero, suficiente para el chatbot |
| `anthropic/claude-haiku` | ~$0.25/1M tokens | Claude barato via OpenRouter |
| `openai/gpt-4o-mini` | ~$0.15/1M tokens | Alternativa OpenAI |

Para cambiar de modelo, solo cambia la propiedad:
```properties
claude.model=meta-llama/llama-3-8b-instruct
```

---

## Como volver a Claude directo (Anthropic)

Cambia **4 lineas** en `application.properties`:

```properties
# Volver a Claude directo
claude.api.url=${CLAUDE_API_URL:https://api.anthropic.com/v1/messages}
claude.api.key=${CLAUDE_API_KEY:sk-ant-TU_KEY_DE_ANTHROPIC}
claude.api.auth-type=${CLAUDE_API_AUTH_TYPE:x-api-key}
claude.model=${CLAUDE_MODEL:claude-haiku-4-5}
```

> **No se requiere recompilar.** El codigo soporta ambos formatos de respuesta automaticamente.

### Obtener API Key de Anthropic
1. Ir a **https://console.anthropic.com/**
2. **API Keys** → **Create Key**

### Modelos de Claude disponibles

| Modelo | Precio | Velocidad |
|--------|--------|-----------|
| `claude-haiku-4-5` | Mas barato | Mas rapido |
| `claude-sonnet-4-6` | Precio medio | Balanceado |
| `claude-opus-4-5` | Mas caro | Mas capaz |

---

## Resumen de propiedades

| Propiedad | OpenRouter | Anthropic directo |
|-----------|-----------|-------------------|
| `claude.api.url` | `https://openrouter.ai/api/v1/chat/completions` | `https://api.anthropic.com/v1/messages` |
| `claude.api.auth-type` | `bearer` | `x-api-key` |
| `claude.api.key` | `sk-or-v1-...` | `sk-ant-...` |
| `claude.model` | `google/gemini-flash-1.5` | `claude-haiku-4-5` |

---

## Modo sin IA (fallback)

Si se deja `claude.api.key` vacio, el sistema opera sin IA:
- **Chatbot**: devuelve preguntas predefinidas estaticas
- **Anonimizacion**: pasa el texto sin modificar

Esto permite usar toda la app sin ninguna API key.
