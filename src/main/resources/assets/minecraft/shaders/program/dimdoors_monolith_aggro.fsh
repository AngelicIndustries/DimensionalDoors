#version 120

// Dimensional Doors: the screen of a player a Monolith is about to take. Red edges pulse with a heartbeat, and the
// picture breaks up in red glitches (torn bands, jumping blocks, split colors, static), more often and harder as
// Intensity grows.

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;

uniform vec2 OutSize;
uniform float Intensity;
uniform float Pulse;
uniform float Clock;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec2 uv = texCoord;
    float frame = floor(Clock * 20.0);

    // Some frames glitch, more of them and harder the stronger it gets
    float burst = step(1.0 - (0.04 + 0.26 * Intensity), hash(vec2(frame, 1.7))) * Intensity;

    // Torn bands slide sideways
    float row = floor(uv.y * 28.0 + hash(vec2(frame, 3.1)) * 4.0);
    float torn = step(0.75, hash(vec2(row, frame)));
    uv.x += torn * (hash(vec2(row, frame + 9.0)) - 0.5) * 0.14 * burst;

    // Blocks of the picture jump, as a broken video stream's do
    vec2 cell = floor(uv * vec2(24.0, 14.0));
    float jumps = step(0.9, hash(cell + vec2(frame * 0.37, frame)));
    uv += jumps * burst * (vec2(hash(cell + 5.0 + frame), hash(cell + 7.0 + frame)) - 0.5) * 0.1;

    // The colors split apart, a little always, a lot in bursts, most at the edges
    vec2 centered = texCoord - 0.5;
    float split = (0.003 * length(centered) * 2.0 + 0.012 * burst) * Intensity;
    vec3 color = vec3(
        texture2D(DiffuseSampler, uv + vec2(split, 0.0)).r,
        texture2D(DiffuseSampler, uv).g,
        texture2D(DiffuseSampler, uv - vec2(split, 0.0)).b);

    // Torn bands sometimes turn to a red negative
    float negative = torn * step(0.85, hash(vec2(row * 1.3, frame + 2.0))) * burst;
    color = mix(color, (1.0 - color) * vec3(0.9, 0.1, 0.1), negative * 0.6);

    // Red edges, beating: the outer part of the screen, the corners most
    float edge = smoothstep(0.7, 1.4, length(centered * 2.0));
    float red = edge * Intensity * (0.35 + 0.65 * Pulse) * 0.85;
    color = mix(color, vec3(0.4, 0.0, 0.01), red) + vec3(0.2, 0.0, 0.0) * red * Pulse;

    // Static, faint always, thick in bursts, and a red cast over bursts
    float grain = hash(texCoord * OutSize + vec2(frame * 13.0, frame * 7.0));
    color += (grain - 0.5) * (0.03 + 0.15 * burst) * Intensity;
    color = mix(color, color * vec3(1.3, 0.4, 0.4), burst * 0.35);

    gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
