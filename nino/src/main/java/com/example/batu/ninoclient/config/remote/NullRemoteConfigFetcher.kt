package com.example.batu.ninoclient.config.remote

import android.content.Context

class NullRemoteConfigFetcher : IRemoteConfigFetcher {

  override fun isLatestVersion(): Boolean = true

  override fun setup(context: Context) {

  }

}