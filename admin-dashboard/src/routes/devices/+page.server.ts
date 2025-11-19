import { db } from '$lib/server/db';
import { devices } from '$lib/server/db/schema';

export const load = async () => {
    const allDevices = await db.select().from(devices).all();
    return {
        devices: allDevices
    };
};
