
import { json } from '@sveltejs/kit';
import { db } from '$lib/server/db';
import { devicePolicies } from '$lib/server/db/schema';
import { eq } from 'drizzle-orm';

export async function POST({ params, request }) {
    try {
        const deviceId = parseInt(params.id);
        const { policyId } = await request.json();

        if (!deviceId || !policyId) {
            return json({ error: 'Missing deviceId or policyId' }, { status: 400 });
        }

        // Check if assignment already exists, if so update, else insert
        // Since we didn't set a primary key on device_policies, we should probably delete old ones first
        // or use a composite key. For simplicity, let's delete old assignment for this device.

        await db.delete(devicePolicies).where(eq(devicePolicies.deviceId, deviceId));

        await db.insert(devicePolicies).values({
            deviceId,
            policyId,
            assignedAt: new Date()
        });

        return json({ success: true });
    } catch (err) {
        console.error(err);
        return json({ error: 'Failed to assign policy' }, { status: 500 });
    }
}
