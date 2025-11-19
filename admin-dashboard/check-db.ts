import { db } from './src/lib/server/db';
import { devices } from './src/lib/server/db/schema';

async function main() {
    const allDevices = await db.select().from(devices).all();
    console.log(JSON.stringify(allDevices, null, 2));
}

main();
