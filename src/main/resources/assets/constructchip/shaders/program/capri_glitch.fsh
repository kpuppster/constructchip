#version 150
// Glitch post effect by Ikeda (Capri mod), used with permission.
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Glitch;
uniform float Red;
uniform float Edge;
uniform float Flash;
uniform float Impact;
uniform float Shake;
uniform float Frame;
in vec2 texCoord;
out vec4 fragColor;
float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
void main() {
    vec2 uv = texCoord;
    float f = Frame;
    if (Shake > 0.0) uv.y += Shake * 0.02 * (mod(floor(f / 1.5), 2.0) < 1.0 ? 1.0 : -1.0) * (0.5 + hash(vec2(floor(f / 1.5), 7.0)));
    float g = Glitch;
    if (g > 0.0) {
        float rowA = floor(uv.y * 22.0), rowB = floor(uv.y * 90.0);
        float bandA = step(1.0 - 0.22 * g, hash(vec2(rowA, f)));
        float bandB = step(1.0 - 0.15 * g, hash(vec2(rowB, f + 3.7)));
        uv.x += (hash(vec2(rowA, f + 1.3)) - 0.5) * 0.06 * g * bandA;
        uv.x += (hash(vec2(rowB, f + 5.1)) - 0.5) * 0.025 * g * bandB;
    }
    float split = 0.006 * g + 0.0015 * Edge;
    vec3 col;
    col.r = texture(DiffuseSampler, uv + vec2(-split, 0.0)).r;
    col.g = texture(DiffuseSampler, uv + vec2(split, 0.0)).g;
    col.b = texture(DiffuseSampler, uv + vec2(split * 1.4, 0.0)).b;
    if (g > 0.0) {
        float colN = floor(uv.x * 160.0);
        float line = step(1.0 - 0.025 * g, hash(vec2(colN, f + 9.1)));
        float seg = step(0.5, hash(vec2(colN, floor(uv.y * 6.0) + f)));
        vec3 pink = hash(vec2(colN, f)) < 0.6 ? vec3(1.0, 0.35, 0.78) : vec3(1.0);
        col = mix(col, pink, line * seg * 0.7);
        col *= 1.0 - 0.07 * g * step(0.5, fract(uv.y * OutSize.y * 0.5));
    }
    if (Red > 0.0) {
        float l = dot(col, vec3(0.3, 0.59, 0.11));
        vec3 red = vec3(0.98, 0.03, 0.18) * (0.25 + 0.95 * l);
        col = mix(col, red, 0.6 * Red);
    }
    if (Edge > 0.0) {
        vec2 p = abs(texCoord - 0.5) * 2.0;
        float e = smoothstep(0.82, 1.0, max(p.x, p.y));
        float n = hash(floor(texCoord * OutSize / 3.0) + f);
        col = mix(col, vec3(0.98, 0.05, 0.25), e * Edge * (0.18 + 0.35 * step(0.75, n)));
    }
    if (Flash > 0.9) col = mix(col, vec3(0.976), 0.97);
    else col = col + (vec3(1.0, 0.85, 0.9) - col) * Flash * 0.7;
    if (Impact > 0.0) {
        vec2 p = (texCoord - 0.5) * vec2(OutSize.x / OutSize.y, 1.0);
        float ang = atan(p.y, p.x);
        float ray = step(0.82, hash(vec2(floor(ang * 18.0), f))) * step(0.04, length(p));
        float l = dot(col, vec3(0.3, 0.59, 0.11));
        col = vec3(max(step(0.62, l), ray));
    }
    fragColor = vec4(col, 1.0);
}
