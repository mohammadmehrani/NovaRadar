package com.irnova.novaradar.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class ScannerRepository_Factory implements Factory<ScannerRepository> {
  @Override
  public ScannerRepository get() {
    return newInstance();
  }

  public static ScannerRepository_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ScannerRepository newInstance() {
    return new ScannerRepository();
  }

  private static final class InstanceHolder {
    private static final ScannerRepository_Factory INSTANCE = new ScannerRepository_Factory();
  }
}
