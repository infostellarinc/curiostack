using CafeMap.Map;
using CafeMap.Player.Services;
using Google.Maps;
using UnityEngine;
using UnityEngine.UI;
using Zenject;

public class AppInstaller : MonoInstaller
{
    public override void InstallBindings()
    {
        var searchBox = GameObject.FindWithTag("SearchBox");

        Container.Bind<MapsService>().FromComponentOnRoot().AsSingle();
        Container.BindInterfacesAndSelfTo<BaseMapLoader>().FromComponentOnRoot().AsSingle();
        Container.BindInterfacesAndSelfTo<DynamicMapsUpdater>().FromComponentOnRoot().AsSingle();

        Container.BindInstance(Camera.main.GetComponent<PanAndZoom>());

        Container.Bind<InputField>().WithId("SearchBox").FromInstance(searchBox.GetComponent<InputField>());

        Container.Bind<TextAsset>().WithId("Secrets").FromResources("Secrets").AsSingle();

        Container.BindInterfacesAndSelfTo<ViewportService>().AsSingle();
        Container.BindInterfacesAndSelfTo<SecretsService>().AsSingle().NonLazy();
        Container.BindInterfacesAndSelfTo<SearchService>().AsSingle().NonLazy();
    }
}