# Generates the electric crackle/zap sounds (our own, CC0): python3 scripts/gen_crackle.py src/main/resources/assets/constructchip/sounds
import numpy as np, subprocess, sys, os
SR=44100
out=sys.argv[1]
def hp(x, a=0.97):  # one-pole high-pass
    y=np.zeros_like(x); prev=0.0; py=0.0
    for i,v in enumerate(x):
        py=a*(py+v-prev); prev=v; y[i]=py
    return y
def crackle(seed, dur, density, hum, burst_len=(0.002,0.012), loud=1.0):
    r=np.random.default_rng(seed); n=int(SR*dur); x=np.zeros(n)
    t=0.0
    while t<dur:
        # clustered pops: short gaps inside clusters, longer gaps between
        t+= r.exponential(1/density) * (0.25 if r.random()<0.6 else 1.6)
        i=int(t*SR)
        if i>=n: break
        L=int(SR*r.uniform(*burst_len)); L=min(L,n-i)
        env=np.exp(-np.linspace(0,7,L))
        amp=r.uniform(0.25,1.0)**1.5
        x[i:i+L]+= r.standard_normal(L)*env*amp
    x=hp(x,0.92)
    tt=np.arange(n)/SR
    buzz=(np.sign(np.sin(2*np.pi*hum*tt))*0.5+np.sin(2*np.pi*hum*2*tt)*0.5)*0.035
    buzz*=np.abs(np.convolve(np.abs(x),np.ones(800)/800,'same'))*6  # buzz follows the crackle
    y=x+buzz
    fade=np.minimum(1,np.minimum(tt/0.005,(dur-tt)/0.06))
    y*=fade
    y=y/np.max(np.abs(y))*0.89*loud
    return y
def zap(seed):
    r=np.random.default_rng(seed); dur=0.9; n=int(SR*dur); tt=np.arange(n)/SR
    c=crackle(seed,dur,140,100,(0.003,0.02))
    noise=hp(r.standard_normal(n),0.85)*np.exp(-tt*9)          # the snap
    sweep=np.sin(2*np.pi*np.cumsum(900*np.exp(-tt*6)+120)/SR)*np.exp(-tt*5)*0.35  # falling discharge tone
    y=c*0.6+noise*0.9+sweep
    y*=np.minimum(1,(dur-tt)/0.1)
    return y/np.max(np.abs(y))*0.9
def save(name,y):
    pcm=(np.clip(y,-1,1)*32767).astype('<i2').tobytes()
    subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-f','s16le','-ar',str(SR),'-ac','1','-i','-',
                    '-c:a','libvorbis','-q:a','4',os.path.join(out,name+'.ogg')],input=pcm,check=True)
for i,(d,den,hum) in enumerate([(0.45,60,60),(0.6,45,50),(0.35,80,60),(0.55,55,55)],1):
    save(f'crackle_{i}',crackle(100+i,d,den,hum))
save('zap',zap(7))
print('ok')
