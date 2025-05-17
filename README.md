TravelTdea - Aplicación de Navegación para Android
TravelTdea es una completa aplicación de navegación para dispositivos Android construida con OpenStreetMap e integración de la API de GraphHopper. La aplicación proporciona una interfaz fácil de usar para buscar ubicaciones, planificar rutas y obtener guías de navegación en tiempo real.

Características
Interfaz de Mapa Interactiva: Construida con OpenStreetMap para datos de mapas detallados y actualizados
Búsqueda de Ubicaciones: Encuentra cualquier lugar en el mundo con funcionalidad de búsqueda por texto
Destinos con un Solo Toque: Establece destinos fácilmente tocando cualquier parte del mapa
Navegación en Tiempo Real: Indicaciones paso a paso con estimaciones de distancia y tiempo
Actualizaciones de Ruta en Vivo: Recálculo de rutas basado en tu posición actual
Seguimiento GPS: Rastreo de ubicación en tiempo real con soporte de brújula
Tarjeta de Información de Ruta: Muestra la distancia restante y el tiempo estimado de llegada
Elementos de UI Plegables: Interfaz limpia con tarjetas de información colapsables

Implementación Técnica
Integración de Mapas
La aplicación utiliza la biblioteca OSMDroid para integrar la funcionalidad de OpenStreetMap, proporcionando una solución de mapeo de código abierto con cobertura global.
Servicios de Ubicación

Utiliza los Servicios de Ubicación de Android para un posicionamiento preciso del usuario
Implementa un manejo adecuado de permisos para acceso a la ubicación
Actualiza la ubicación del usuario en tiempo real con tasas de actualización configurables

Motor de Rutas

Se conecta a la API de rutas de GraphHopper para el cálculo de trayectos
Procesa datos de rutas para mostrar el camino óptimo entre puntos
Maneja actualizaciones de ruta cuando el usuario se desvía del camino calculado

Geocodificación y Búsqueda

Implementa la integración de la API Nominatim para convertir nombres de lugares a coordenadas geográficas
Proporciona geocodificación inversa para identificar nombres de ubicaciones al seleccionar puntos en el mapa
Implementación adecuada del user-agent para respetar las políticas de uso de la API

Interfaz de Usuario

Interfaz limpia e intuitiva con tarjeta de información plegable
Animaciones suaves para transiciones de UI
Actualizaciones de distancia y tiempo en tiempo real basadas en la ubicación actual

Claves de API y Dependencias
Claves de API

API de GraphHopper: Se requiere una clave de API válida para la funcionalidad de rutas. La implementación actual utiliza una clave de demostración que tiene solicitudes limitadas. Debes reemplazarla con tu propia clave de API para uso en producción.

Dependencias
gradledependencies {
    // OSMDroid para funcionalidad de mapas
    implementation 'org.osmdroid:osmdroid-android:6.1.13'
    
    // OkHttp para peticiones de red
    implementation 'com.squareup.okhttp3:okhttp:4.9.3'
    
    // Dependencias básicas de Android
    implementation 'androidx.appcompat:appcompat:1.4.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.3'
}
Permisos Requeridos
xml<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="28" />
Instalación y Configuración

Clona el repositorio:
git clone https://github.com/tunombredeusuario/traveltdea.git

Abre el proyecto en Android Studio
Reemplaza la clave de API de GraphHopper en MapActivity.java:
javaString apiKey = "SuscriptionKey";

Compila y ejecuta la aplicación en tu dispositivo Android o emulador

Guía de Uso
Establecer un Destino

Búsqueda: Ingresa un nombre de ubicación en la barra de búsqueda en la parte superior de la pantalla y presiona Enter
Toque en el Mapa: Toca cualquier lugar en el mapa para establecer esa ubicación como tu destino

Navegación

Una vez establecido un destino, la aplicación calculará la mejor ruta desde tu ubicación actual
La tarjeta de información en la parte inferior muestra la distancia restante y el tiempo estimado de llegada
Sigue la ruta azul mostrada en el mapa para llegar a tu destino

Controles de Interfaz

Colapsar/Expandir Tarjeta: Toca el botón de flecha para ocultar o mostrar la tarjeta de información
Controles del Mapa: Usa gestos de pellizco para hacer zoom, arrastra para desplazar la vista del mapa
Ubicación Actual: El punto azul representa tu posición actual en el mapa
Brújula: El mapa girará según la orientación de tu dispositivo cuando esté habilitado

Consideraciones de Rendimiento

La aplicación implementa actualizaciones eficientes de ubicación para minimizar el uso de batería:

Actualizaciones activadas por movimientos significativos (mínimo 10 metros)
Actualizaciones basadas en tiempo a intervalos razonables (5 segundos)
Umbral de recálculo de ruta de 50 metros de desviación


Optimización del uso de red:

Las rutas solo se recalculan cuando es necesario
Las consultas de búsqueda se limitan a resultados únicos para reducir el uso de datos

Contribuciones
¡Las contribuciones son bienvenidas! Si deseas mejorar TravelTdea, por favor:


Licencia
Este proyecto está licenciado bajo la Licencia libre de uso.

Agradecimientos
OpenStreetMap por proporcionar datos de mapas gratuitos
OSMDroid por la biblioteca OpenStreetMap para Android
GraphHopper por la API de rutas
Nominatim por los servicios de geocodificación
