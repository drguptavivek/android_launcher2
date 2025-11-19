<script lang="ts">
    import { enhance } from "$app/forms";
    import type { PageData } from "./$types";

    let { data } = $props();
    let creating = $state(false);
</script>

<div class="space-y-6">
    <div class="flex items-center justify-between">
        <div>
            <h1 class="text-2xl font-bold text-gray-900">Users</h1>
            <p class="mt-1 text-sm text-gray-500">
                Manage users who can log in to devices.
            </p>
        </div>
        <button
            onclick={() => (creating = !creating)}
            class="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors"
        >
            {creating ? "Cancel" : "Add User"}
        </button>
    </div>

    {#if creating}
        <div
            class="bg-white shadow-sm rounded-xl border border-gray-200 p-6 mb-6"
        >
            <h2 class="text-lg font-medium text-gray-900 mb-4">
                Create New User
            </h2>
            <form
                method="POST"
                action="?/create"
                use:enhance={() => {
                    return async ({ result, update }) => {
                        if (result.type === "success") {
                            creating = false;
                        }
                        await update();
                    };
                }}
            >
                <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
                    <div>
                        <label
                            for="username"
                            class="block text-sm font-medium text-gray-700"
                            >Username</label
                        >
                        <input
                            type="text"
                            name="username"
                            id="username"
                            required
                            class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border p-2"
                        />
                    </div>
                    <div>
                        <label
                            for="password"
                            class="block text-sm font-medium text-gray-700"
                            >Password</label
                        >
                        <input
                            type="password"
                            name="password"
                            id="password"
                            required
                            class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border p-2"
                        />
                    </div>
                    <div>
                        <label
                            for="role"
                            class="block text-sm font-medium text-gray-700"
                            >Role</label
                        >
                        <select
                            name="role"
                            id="role"
                            class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border p-2"
                        >
                            <option value="child">Child</option>
                            <option value="parent">Parent</option>
                            <option value="admin">Admin</option>
                        </select>
                    </div>
                </div>
                <div class="mt-6 flex justify-end">
                    <button
                        type="submit"
                        class="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors"
                    >
                        Create User
                    </button>
                </div>
            </form>
        </div>
    {/if}

    <div
        class="bg-white shadow-sm rounded-xl border border-gray-200 overflow-hidden"
    >
        <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                    <tr>
                        <th
                            scope="col"
                            class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                            >User</th
                        >
                        <th
                            scope="col"
                            class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                            >Role</th
                        >
                        <th
                            scope="col"
                            class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                            >Created At</th
                        >
                        <th scope="col" class="relative px-6 py-3">
                            <span class="sr-only">Actions</span>
                        </th>
                    </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                    {#each data.users as user}
                        <tr class="hover:bg-gray-50 transition-colors">
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div class="flex items-center">
                                    <div
                                        class="flex-shrink-0 h-10 w-10 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-600"
                                    >
                                        <span class="font-medium text-sm"
                                            >{user.username
                                                .substring(0, 2)
                                                .toUpperCase()}</span
                                        >
                                    </div>
                                    <div class="ml-4">
                                        <div
                                            class="text-sm font-medium text-gray-900"
                                        >
                                            {user.username}
                                        </div>
                                        <div class="text-sm text-gray-500">
                                            {user.id}
                                        </div>
                                    </div>
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span
                                    class="px-2.5 py-0.5 rounded-full text-xs font-medium
                                    {user.role === 'admin'
                                        ? 'bg-purple-100 text-purple-800'
                                        : user.role === 'parent'
                                          ? 'bg-blue-100 text-blue-800'
                                          : 'bg-green-100 text-green-800'}"
                                >
                                    {user.role.charAt(0).toUpperCase() +
                                        user.role.slice(1)}
                                </span>
                            </td>
                            <td
                                class="px-6 py-4 whitespace-nowrap text-sm text-gray-500"
                            >
                                {user.createdAt?.toLocaleString()}
                            </td>
                            <td
                                class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium"
                            >
                                <form
                                    method="POST"
                                    action="?/delete"
                                    use:enhance
                                >
                                    <input
                                        type="hidden"
                                        name="id"
                                        value={user.id}
                                    />
                                    <button
                                        type="submit"
                                        class="text-red-600 hover:text-red-900"
                                        onclick={(e) => {
                                            if (!confirm("Are you sure?"))
                                                e.preventDefault();
                                        }}>Delete</button
                                    >
                                </form>
                            </td>
                        </tr>
                    {/each}
                </tbody>
            </table>
        </div>
        {#if data.users.length === 0}
            <div class="text-center py-12">
                <svg
                    class="mx-auto h-12 w-12 text-gray-400"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                >
                    <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
                    />
                </svg>
                <h3 class="mt-2 text-sm font-medium text-gray-900">No users</h3>
                <p class="mt-1 text-sm text-gray-500">
                    Get started by creating a new user.
                </p>
            </div>
        {/if}
    </div>
</div>
