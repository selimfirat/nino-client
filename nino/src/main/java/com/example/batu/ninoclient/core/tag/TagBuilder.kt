package com.example.batu.ninoclient.core.tag

import com.github.bijoysingh.starter.util.RandomHelper
import com.example.batu.ninoclient.database.room.tag.Tag

class TagBuilder() {
  fun emptyTag(): Tag {
    val tag = Tag()
    tag.uid = 0
    tag.title = ""
    tag.uuid = RandomHelper.getRandomString(24)
    return tag
  }

  fun copy(tagContainer: ITagContainer): Tag {
    val tag = emptyTag()
    tag.uuid = tagContainer.uuid()
    tag.title = tagContainer.title()
    return tag
  }
}