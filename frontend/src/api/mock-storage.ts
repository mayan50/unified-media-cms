import api from './index'

// Mock data for storage browse when backend is unavailable
const MOCK_STORAGE_NODES = [
  { id: 'sn-001', name: '本地 NAS', providerType: 'LOCAL', isReadonly: false },
  { id: 'sn-002', name: 'MinIO 对象存储', providerType: 'MINIO', isReadonly: true },
]

const MOCK_FS: Record<string, { name: string; is_dir: boolean; path: string }[]> = {
  '/': [
    { name: 'books', is_dir: true, path: '/books' },
    { name: 'movies', is_dir: true, path: '/movies' },
    { name: 'music', is_dir: true, path: '/music' },
    { name: 'downloads', is_dir: true, path: '/downloads' },
    { name: 'readme.md', is_dir: false, path: '/readme.md' },
  ],
  '/books': [
    { name: 'incoming', is_dir: true, path: '/books/incoming' },
    { name: 'library', is_dir: true, path: '/books/library' },
    { name: 'unsorted', is_dir: true, path: '/books/unsorted' },
  ],
  '/books/incoming': [
    { name: '三体.epub', is_dir: false, path: '/books/incoming/三体.epub' },
    { name: '百年孤独.txt', is_dir: false, path: '/books/incoming/百年孤独.txt' },
  ],
  '/books/library': [
    { name: '科幻', is_dir: true, path: '/books/library/科幻' },
    { name: '文学', is_dir: true, path: '/books/library/文学' },
  ],
  '/books/library/科幻': [
    { name: '基地.epub', is_dir: false, path: '/books/library/科幻/基地.epub' },
    { name: '银河帝国.txt', is_dir: false, path: '/books/library/科幻/银河帝国.txt' },
  ],
  '/books/library/文学': [
    { name: '红楼梦.epub', is_dir: false, path: '/books/library/文学/红楼梦.epub' },
  ],
  '/books/unsorted': [
    { name: '未知文件.txt', is_dir: false, path: '/books/unsorted/未知文件.txt' },
  ],
  '/movies': [
    { name: 'incoming', is_dir: true, path: '/movies/incoming' },
    { name: 'library', is_dir: true, path: '/movies/library' },
  ],
  '/movies/incoming': [
    { name: 'Inception.2010.mkv', is_dir: false, path: '/movies/incoming/Inception.2010.mkv' },
  ],
  '/movies/library': [
    { name: 'Sci-Fi', is_dir: true, path: '/movies/library/Sci-Fi' },
  ],
  '/movies/library/Sci-Fi': [
    { name: 'Interstellar.2014.mkv', is_dir: false, path: '/movies/library/Sci-Fi/Interstellar.2014.mkv' },
  ],
  '/music': [
    { name: 'flac', is_dir: true, path: '/music/flac' },
    { name: 'mp3', is_dir: true, path: '/music/mp3' },
  ],
  '/music/flac': [],
  '/music/mp3': [],
  '/downloads': [
    { name: 'temp.zip', is_dir: false, path: '/downloads/temp.zip' },
  ],
}

// Install mock interceptor — returns mock data when backend returns 404/network error
let installed = false

export function installStorageMock() {
  if (installed) return
  installed = true

  api.interceptors.request.use((config) => {
    const url = config.url || ''

    // Mock: GET /storage-nodes
    if (url === '/storage-nodes' && config.method === 'get') {
      config.adapter = () =>
        Promise.resolve({ data: MOCK_STORAGE_NODES, status: 200, statusText: 'OK', headers: {}, config } as any)
      return config
    }

    // Mock: GET /storage-nodes/{id}/browse?path=xxx
    const browseMatch = url.match(/^\/storage-nodes\/([^/]+)\/browse$/)
    if (browseMatch && config.method === 'get') {
      const path = (config.params?.path as string) || '/'
      config.adapter = () =>
        Promise.resolve({ data: MOCK_FS[path] || [], status: 200, statusText: 'OK', headers: {}, config } as any)
      return config
    }

    return config
  })
}
