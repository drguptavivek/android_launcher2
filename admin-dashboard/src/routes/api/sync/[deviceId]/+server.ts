
import { json } from '@sveltejs/kit';
import { db } from '$lib/server/db';
import { devicePolicies, policies } from '$lib/server/db/schema';
import { eq } from 'drizzle-orm';

export async function GET({ params }) {
    try {
        const deviceId = parseInt(params.deviceId);

        if (!deviceId) {
            return json({ error: 'Invalid deviceId' }, { status: 400 });
        }

        // Join devicePolicies and policies to get the config
        const result = await db.select({
            config: policies.config,
            updatedAt: policies.createdAt // Using creation time as version for now
        })
            .from(devicePolicies)
            .innerJoin(policies, eq(devicePolicies.policyId, policies.id))
            .where(eq(devicePolicies.deviceId, deviceId))
            .limit(1);

        if (result.length === 0) {
            return json({ error: 'No policy assigned' }, { status: 404 });
        }

        return json(result[0]);
    } catch (err) {
        console.error(err);
        return json({ error: 'Failed to fetch policy' }, { status: 500 });
    }
}
