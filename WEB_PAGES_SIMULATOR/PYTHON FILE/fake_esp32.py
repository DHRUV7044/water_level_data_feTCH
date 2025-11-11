from flask import Flask, jsonify, request, render_template_string, make_response
import random, time

app = Flask(__name__)

# Tank configuration
TANK_HEIGHT_MM = 1200  # Total tank height in mm
MAX_WATER_LEVEL_MM = 950  # Height at which tank is considered 100% full
current_distance = 250.0  # Default distance from sensor in mm

HTML = """
<!DOCTYPE html><html><head><meta charset='utf-8'>
<meta http-equiv='refresh' content='30'>
<meta name='viewport' content='width=device-width,initial-scale=1'>
<title>Water Tank Level</title>
<style>
body{font-family:Arial;text-align:center;background:#f6f8fc;margin:0;padding:10px;}
.card{background:#fff;max-width:360px;margin:auto;padding:10px;border-radius:10px;
box-shadow:0 0 10px rgba(0,0,0,0.1);}svg{width:200px;height:240px;}
.wave{animation:move 6s linear infinite;}@keyframes move{0%{transform:translateX(0);}100%{transform:translateX(-100px);}}
.btn{background:#1976d2;color:#fff;border:none;border-radius:6px;padding:8px 14px;cursor:pointer;}
.spinner{border:3px solid rgba(255,255,255,0.25);border-top:3px solid #fff;border-radius:50%;
width:18px;height:18px;display:inline-block;animation:spin 1s linear infinite;margin-left:8px;}@keyframes spin{to{transform:rotate(360deg);}}
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
"""

def calculate_values(distance_mm):
    """Calculate water level and percentage based on distance measurement."""
    water_level_mm = TANK_HEIGHT_MM - distance_mm
    # Calculate percentage based on MAX_WATER_LEVEL_MM instead of TANK_HEIGHT_MM
    percent = min(100.0, (water_level_mm / MAX_WATER_LEVEL_MM) * 100.0)
    return water_level_mm, percent

@app.route('/')
def index():
    # Calculate current measurements
    water_level_mm, percent = calculate_values(current_distance)
    # Prepare the HTML by replacing placeholders expected by the client-side JS
    html = HTML.replace('%LEVEL%', f"{water_level_mm:.1f}")
    html = html.replace('%PCT%', f"{percent:.1f}")
    html = html.replace('%DIST%', f"{current_distance:.1f}")
    response = make_response(html)
    response.headers['Content-Type'] = 'text/html; charset=utf-8'
    return response

@app.route('/height')
def height():
    global current_distance
    # Add small random variation to make it more realistic
    current_distance += random.uniform(-1, 1)
    current_distance = max(0, min(TANK_HEIGHT_MM, current_distance))
    
    water_level_mm, percent = calculate_values(current_distance)
    
    return jsonify({
        "distance_mm": current_distance,
        "water_level_mm": water_level_mm,
        "percent": percent
    })

@app.route('/set', methods=['POST'])
def set_distance():
    global current_distance
    body = request.get_json(force=True, silent=True) or {}
    distance = float(body.get("distance_mm", current_distance))
    if 0 <= distance <= TANK_HEIGHT_MM:
        current_distance = distance
    water_level_mm, percent = calculate_values(current_distance)
    return jsonify({
        "distance_mm": current_distance,
        "water_level_mm": water_level_mm,
        "percent": percent
    })

@app.route('/random')
def random_distance():
    global current_distance
    # Generate random distance that will result in reasonable water levels
    current_distance = round(random.uniform(200, 400), 1)  # Keep water level mostly high
    water_level_mm, percent = calculate_values(current_distance)
    return jsonify({
        "distance_mm": current_distance,
        "water_level_mm": water_level_mm,
        "percent": percent
    })

if __name__ == '__main__':
    import socket
    hostname = socket.gethostname()
    try:
        # Get IP address
        ip_address = socket.gethostbyname(hostname)
        print(f"Server running on IP: {ip_address}")
        print("To access from Android app, make sure your phone is on the same network")
        print("Use URL: http://<your-computer-ip>/height")
    except:
        print("Could not determine IP address")
    
    app.run(host='0.0.0.0', port=80, debug=True)
