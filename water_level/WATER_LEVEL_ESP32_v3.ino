#include <WiFi.h>
#include <WebServer.h>

const char* ssid = "GIGBEST";
const char* password = "22603636";

#define TRIG_PIN 4
#define ECHO_PIN 15
#define TANK_HEIGHT_MM 1200.0
#define FULL_LEVEL_MM 950.0
#define TIMEOUT_US 30000UL

WebServer server(80);

float measureDistanceMM() {
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  unsigned long d = pulseIn(ECHO_PIN, HIGH, TIMEOUT_US);
  if (d == 0) return -1;
  return (d * 0.343f) / 2.0f;
}

void sendJSON() {
  float dist = measureDistanceMM();
  if (dist < 0) dist = TANK_HEIGHT_MM;
  float level = TANK_HEIGHT_MM - dist;
  if (level < 0) level = 0;
  if (level > FULL_LEVEL_MM) level = FULL_LEVEL_MM;
  float pct = (level / FULL_LEVEL_MM) * 100.0;
  String json = "{\"distance_mm\":" + String(dist, 1) +
                ",\"water_level_mm\":" + String(level, 1) +
                ",\"percent\":" + String(pct, 1) + "}";
  server.send(200, "application/json", json);
}

void sendPage() {
  float dist = measureDistanceMM();
  if (dist < 0) dist = TANK_HEIGHT_MM;
  float level = TANK_HEIGHT_MM - dist;
  if (level < 0) level = 0;
  if (level > FULL_LEVEL_MM) level = FULL_LEVEL_MM;
  float pct = (level / FULL_LEVEL_MM) * 100.0;

  String html = R"rawliteral(
<!DOCTYPE html><html><head><meta charset='utf-8'>
<meta http-equiv='refresh' content='30'>
<meta name='viewport' content='width=device-width,initial-scale=1'>
<title>Water Tank Level</title>
<style>
body{font-family:Arial;text-align:center;background:#f6f8fc;margin:0;padding:10px;}
.card{background:#fff;max-width:360px;margin:auto;padding:10px;border-radius:10px;
box-shadow:0 0 10px rgba(0,0,0,0.1);}
svg{width:200px;height:240px;}
.wave{animation:move 6s linear infinite;}
@keyframes move{0%{transform:translateX(0);}100%{transform:translateX(-100px);}}
.btn{background:#1976d2;color:#fff;border:none;border-radius:6px;padding:8px 14px;cursor:pointer;}
.spinner{border:3px solid rgba(255,255,255,0.25);border-top:3px solid #fff;border-radius:50%;
width:18px;height:18px;display:inline-block;animation:spin 1s linear infinite;margin-left:8px;}
@keyframes spin{to{transform:rotate(360deg);}}
</style></head><body>
<div class='card'>
<h2>ESP32 Water Tank Level</h2>
<svg viewBox='0 0 100 220'>
<defs>
<linearGradient id='g' x1='0' x2='0' y1='0' y2='1'>
<stop offset='0%' stop-color='#7ecbff'/><stop offset='100%' stop-color='#0b75d1'/>
</linearGradient>
<clipPath id='clip'><rect x='12' y='10' width='76' height='200' rx='8'/></clipPath>
</defs>
<rect x='12' y='10' width='76' height='200' rx='8' stroke='#333' stroke-width='2' fill='none'/>
<g clip-path='url(#clip)'>
<rect id='fill' x='12' y='210' width='76' height='0' fill='url(#g)'/>
<path class='wave' d='M0,200 Q25,190 50,200 T100,200 V220 H0 Z' fill='url(#g)' opacity='0.8'/>
</g>
<rect x='12' y='10' width='76' height='200' rx='8' stroke='#333' stroke-width='2' fill='none'/>
</svg>
<p><b>Level:</b> <span id='lvl'>0</span> mm (<span id='pct'>0</span>%)</p>
<p><small>Distance: <span id='dist'>0</span> mm</small></p>
<button id='btn' class='btn'>Measure Now</button><span id='sp' style='display:none' class='spinner'></span>
<p><small>Auto-refresh every 30s</small></p>
</div>
<script>
let lvl=%LEVEL%,pct=%PCT%,dist=%DIST%;
function updateUI(){document.getElementById('lvl').innerText=lvl.toFixed(1);
document.getElementById('pct').innerText=pct.toFixed(1);
document.getElementById('dist').innerText=dist.toFixed(1);
let fillH=pct*2.0;let y=210-fillH;document.getElementById('fill').setAttribute('y',y);
document.getElementById('fill').setAttribute('height',fillH);}
updateUI();
document.getElementById('btn').onclick=()=>{
  let b=document.getElementById('btn'),s=document.getElementById('sp');
  b.disabled=true;s.style.display='inline-block';
  fetch('/height').then(r=>r.json()).then(j=>{
    lvl=j.water_level_mm;pct=j.percent;dist=j.distance_mm;updateUI();
  }).finally(()=>{b.disabled=false;s.style.display='none';});
};
</script></body></html>
)rawliteral";

  html.replace("%LEVEL%", String(level, 1));
  html.replace("%PCT%", String(pct, 1));
  html.replace("%DIST%", String(dist, 1));
  server.send(200, "text/html", html);
}

void setup() {
  Serial.begin(115200);
  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nIP: " + WiFi.localIP().toString());
  server.on("/", sendPage);
  server.on("/height", sendJSON);
  server.begin();
}

void loop() { server.handleClient(); }
