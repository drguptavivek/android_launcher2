
export const load = async ({ fetch }) => {
    const response = await fetch('/api/policies');
    const policies = await response.json();
    return { policies };
};
