import { createCanvas } from 'canvas';
import { NativeImage, nativeImage } from 'electron';

type BadgeDescriptor = {
  nativeImage: NativeImage | null;
  description: string;
};

class Badge {
  private maxValue = 9;
  private badgesCache = new Map<number, NativeImage>();

  preloadBadges(): void {
    for (let i = 1; i < this.maxValue + 2; i++) {
      this.badgesCache.set(i, this.generateBadge(i));
    }
  }

  takeCachedBadge(count: number): BadgeDescriptor {
    const key = count <= 0 ? 0 : Math.min(count, this.maxValue + 1);
    return {
      nativeImage: this.badgesCache.get(key) || null,
      description: `${Math.max(count, 0)} notifications`,
    };
  }

  private generateBadge(count: number): NativeImage {
    const text = count > 9 ? '9+' : count.toString();
    const size = 16;
    const center = size / 2;

    const canvas = createCanvas(size, size);
    const ctx = canvas.getContext('2d');

    ctx.beginPath();
    ctx.arc(center, center, center, 0, Math.PI * 2);
    ctx.fillStyle = '#e74c3c';
    ctx.fill();

    const fontSize = text.length > 1 ? size * 0.55 : size * 0.65;

    ctx.fillStyle = '#ffffff';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.font = `bold ${fontSize}px Arial`;
    ctx.fillText(text, center, center);

    const buffer = canvas.toBuffer('image/png');
    return nativeImage.createFromBuffer(buffer);
  }
}

export { Badge };
