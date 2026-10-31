import { readFile, writeFile } from 'node:fs/promises'
import { Resvg } from '@resvg/resvg-js'

const svg = await readFile(new URL('../public/icon-maskable.svg', import.meta.url), 'utf8')
for (const size of [180, 192, 512]) {
  const result = new Resvg(svg, { fitTo: { mode: 'width', value: size } }).render().asPng()
  await writeFile(new URL(`../public/icon-${size}.png`, import.meta.url), result)
}
console.log('Generated 180, 192 and 512 pixel app icons from the HealthLink vector mark.')
