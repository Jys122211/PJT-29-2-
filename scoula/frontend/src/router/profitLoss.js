export default [

    {
        path: '/comparison/input',
        name: 'comparisonInput',
        component: () => import('../pages/comparison/InputPage.vue'),
    },
    {
        path: '/comparison/history',
        name: 'comparisonHistory',
        component: () => import('../pages/comparison/HistoryPage.vue'),
    },
    {
        path: '/asset/register',
        name: 'assetRegister',
        component: () => import('../pages/asset/RegisterPage.vue'),
    },
    {
        path: '/asset/list',
        name: 'assetList',
        component: () => import('../pages/asset/ListPage.vue'),

    },
    {
        path: '/profile',
        name: 'profile',
        component: () => import('../pages/auth/ProfilePage.vue'),
    }
];
