import { db } from '$lib/server/db';
import { devices, policies } from '$lib/server/db/schema';
import { desc } from 'drizzle-orm';

export const load = async () => {
    const allDevices = await db.select().from(devices).all();
    const allPolicies = await db.select().from(policies).orderBy(desc(policies.createdAt)).all();
    return {
        devices: allDevices,
        policies: allPolicies
    };
};
