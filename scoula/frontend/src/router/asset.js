export default [
  {
    path: '/asset/register',
    name: 'assetRegister',
    component: () => import('../pages/asset/AssetRegisterPage.vue'),
    meta: {
      layout: 'mobile',
    },
  },
  {
    path: '/asset/list',
    name: 'assetList',
    component: () => import('../pages/asset/AssetListPage.vue'),
    meta: {
      layout: 'mobile',
    },
  },
  {
    path: '/asset/edit/:userDepositId',
    name: 'assetEdit',
    component: () => import('../pages/asset/AssetEditPage.vue'),
    meta: {
      layout: 'mobile',
    },
  },
];
