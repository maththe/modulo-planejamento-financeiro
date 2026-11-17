import { OfflineQueueItem, SyncItemStatus } from '../types';

const DB_NAME = 'planejamento_financeiro_offline';
const DB_VERSION = 1;
const STORE_NAME = 'fila_lancamentos';

function abrirBanco(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, { keyPath: 'chaveIdempotencia' });
        store.createIndex('produtorId', 'produtorId', { unique: false });
        store.createIndex('status', 'status', { unique: false });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

export async function enfileirarOffline(item: OfflineQueueItem): Promise<void> {
  const db = await abrirBanco();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    tx.objectStore(STORE_NAME).put(item);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function listarFilaOffline(produtorId: string): Promise<OfflineQueueItem[]> {
  const db = await abrirBanco();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly');
    const index = tx.objectStore(STORE_NAME).index('produtorId');
    const req = index.getAll(produtorId);
    req.onsuccess = () => resolve(req.result || []);
    req.onerror = () => reject(req.error);
  });
}

export async function atualizarStatusNaFila(
  chaveIdempotencia: string,
  status: SyncItemStatus,
  versaoCliente?: number
): Promise<void> {
  const db = await abrirBanco();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    const getReq = store.get(chaveIdempotencia);
    getReq.onsuccess = () => {
      const item = getReq.result as OfflineQueueItem | undefined;
      if (item) {
        item.status = status;
        if (versaoCliente !== undefined) {
          item.versaoCliente = versaoCliente;
        }
        store.put(item);
      }
    };
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function removerDaFila(chaveIdempotencia: string): Promise<void> {
  const db = await abrirBanco();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    tx.objectStore(STORE_NAME).delete(chaveIdempotencia);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}
