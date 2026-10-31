import openapiTS, { astToString } from 'openapi-typescript'
import { mkdir, readFile, writeFile } from 'node:fs/promises'

// Default generation is reproducible and works without a running backend.
const snapshot = new URL('../openapi/healthlink.json', import.meta.url)
const source = process.argv[2]
let schema
if (source) {
  const response = await fetch(source, { signal: AbortSignal.timeout(15_000) })
  if (!response.ok) throw new Error(`OpenAPI download failed (${response.status})`)
  schema = await response.json()
  await mkdir(new URL('../openapi/', import.meta.url), { recursive: true })
  await writeFile(snapshot, JSON.stringify(schema, null, 2) + '\n')
} else schema = JSON.parse(await readFile(snapshot, 'utf8'))
const ast = await openapiTS(schema)
await writeFile(new URL('../src/lib/api/schema.d.ts', import.meta.url), astToString(ast))
console.log('Generated API contract types in src/lib/api/schema.d.ts')
