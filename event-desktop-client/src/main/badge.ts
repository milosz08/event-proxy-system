import { createCanvas, loadImage } from 'canvas';
import type { NativeImage } from 'electron';
import { nativeImage } from 'electron';

type BadgeDescriptor = {
  nativeImage: NativeImage | null;
  description: string;
};

class Badge {
  private readonly badgesCache = new Map<number, NativeImage>();
  private readonly trayIconCache = new Map<boolean, NativeImage>();

  private maxValue = 9;

  async preloadBadges(iconPath: string): Promise<void> {
    for (let i = 1; i < this.maxValue + 2; i++) {
      this.badgesCache.set(i, this.generateBadge(i));
    }
    const baseIcon = nativeImage.createFromPath(iconPath);
    this.trayIconCache.set(false, baseIcon);
    this.trayIconCache.set(true, await this.generateTrayIconWithDot(baseIcon.toPNG()));
  }

  takeCachedBadge(count: number): BadgeDescriptor {
    const key = count <= 0 ? 0 : Math.min(count, this.maxValue + 1);
    return {
      nativeImage: this.badgesCache.get(key) || null,
      description: `${Math.max(count, 0)} notifications`,
    };
  }

  getTrayIcon(hasUnread: boolean): NativeImage {
    return this.trayIconCache.get(hasUnread) || this.trayIconCache.get(false)!;
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

  private async generateTrayIconWithDot(iconData: Buffer): Promise<NativeImage> {
    const size = 32;
    const canvas = createCanvas(size, size);
    const ctx = canvas.getContext('2d');
    const img = await loadImage(iconData);
    ctx.drawImage(img, 0, 0, size, size);
    ctx.beginPath();
    ctx.arc(26, 6, 6, 0, Math.PI * 2);
    ctx.fillStyle = '#e74c3c';
    ctx.fill();
    return nativeImage.createFromBuffer(canvas.toBuffer('image/png'));
  }
}

export { Badge };
