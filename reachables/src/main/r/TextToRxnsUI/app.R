##########################################################################
#                                                                        #
#  This file is part of the 20n/act project.                             #
#  20n/act enables DNA prediction for synthetic biology/bioengineering.  #
#  Copyright (C) 2017 20n Labs, Inc.                                     #
#                                                                        #
#  Please direct all queries to act@20n.com.                             #
#                                                                        #
#  This program is free software: you can redistribute it and/or modify  #
#  it under the terms of the GNU General Public License as published by  #
#  the Free Software Foundation, either version 3 of the License, or     #
#  (at your option) any later version.                                   #
#                                                                        #
#  This program is distributed in the hope that it will be useful,       #
#  but WITHOUT ANY WARRANTY; without even the implied warranty of        #
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         #
#  GNU General Public License for more details.                          #
#                                                                        #
#  You should have received a copy of the GNU General Public License     #
#  along with this program.  If not, see <http://www.gnu.org/licenses/>. #
#                                                                        #
##########################################################################

# Shiny library (should already be loaded when app started)
require(shiny)
# Scala interpreter in R. Gives us access to all our Java functionalities
library(rscala)
# Logging library (yay!)
library(logging)
basicConfig('DEBUG')

source("text_to_rxns.R")


chemStructureCacheFolder <- "test2rxns.chem.structs"
emptyPNG <- "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="

server <- function(input, output, session) {
  reactions <- reactive({
    shiny::validate(
      need(input$text != "" || input$url != "" || !is.null(input$pdf), "Please input text!")
    )

    acc <- c()

    if (input$text != "") {
      rxns <- extractFromPlainText(input$text)
    } else if (input$url != "") {
      rxns <- extractFromURL(input$url)
    } else if (!is.null(input$pdf)) {
      loginfo("PDF input path: ", input$pdf$datapath)
      rxns <- extractFromPDF(input$pdf$datapath)
    }

    if (rxns$size() > 0) {
      loginfo("Found %d reactions.", length(rxn))

      for (rxn in rxns) {

        rxnDesc <- rxn$apply(0L)
        rxnImg <- rxn$apply(1L)
        newR <- c(rxnDesc, rxnImg)
        acc <- cbind(acc, newR)
      }
    } else {
      # we need to add an empty column, other exceptions are thrown downstream
      acc <- cbind(acc, c("No reactions extracted!", ""))
    }

    acc
  })

  output$reaction_1 <- renderImage({
    rr <- reactions()
    if (ncol(rr) >= 1) {
      rxnid <- 1
      desc <- rr[1,rxnid]
      list(src = rr[2,rxnid],
           contentType = "image/png",
           height = "400px",
           alt = "")
    } else {
      list(src = emptyPNG, contentType = "image/png", height = "0px")
    }
  }, deleteFile = FALSE)

  output$reaction_desc_1 <- renderUI({
    rr <- reactions()
    if (ncol(rr) >= 1) {
      rxnid <- 1
      desc <- paste(b(sprintf("Reaction %d", rxnid)), rr[1,rxnid])
      HTML(desc)
    } else {
      HTML("")
    }
  })

  output$reaction_2 <- renderImage({
    rr <- reactions()
    if (ncol(rr) >= 2) {
      rxnid <- 2
      desc <- rr[1,rxnid]
      list(src = rr[2,rxnid],
           contentType = "image/png",
           height = "400px",
           alt = "")
    } else {
      list(src = emptyPNG, contentType = "image/png", height = "0px")
    }
  }, deleteFile = FALSE)

  output$reaction_desc_2 <- renderUI({
    rr <- reactions()
    if (ncol(rr) >= 2) {
      rxnid <- 2
      desc <- paste(b(sprintf("Reaction %d", rxnid)), rr[1,rxnid])
      HTML(desc)
    } else {
      HTML("")
    }
  })

  output$reaction_3 <- renderImage({
    rr <- reactions()
    if (ncol(rr) >= 3) {
      rxnid <- 3
      desc <- rr[1,rxnid]
      list(src = rr[2,rxnid],
           contentType = "image/png",
           height = "400px",
           alt = "")
    } else {
      list(src = emptyPNG, contentType = "image/png", height = "0px")
    }
  }, deleteFile = FALSE)

  output$reaction_desc_3 <- renderUI({
    rr <- reactions()
    if (ncol(rr) >= 3) {
      rxnid <- 3
      desc <- paste(b(sprintf("Reaction %d", rxnid)), rr[1,rxnid])
      HTML(desc)
    } else {
      HTML("")
    }
  })

  output$over_flow <- renderUI({
    rr <- reactions()
    descs <- c()
    num_overflow <- ncol(rr) - 3
    if (num_overflow > 0) {
      descs <- c(descs, paste("<b><br/><br/>", num_overflow, " more reaction(s):</b>", "<br/>"))
      for (i in 4:ncol(rr)) {
        desc <- paste("<b>Reaction ", i, "</b> ", rr[1,i])
        descs <- c(descs, desc)
      }
      overflow <- paste(descs, collapse="<br/>")
      print(overflow)
      HTML(overflow)
    } else {
      HTML("")
    }
  })
}

ui <- pageWithSidebar(
  headerPanel('20n Biochemical Reactions Miner'),
  sidebarPanel(
    textInput("text", label = "Biochemical text", value = ""),
    textInput("url", label = "Internet location of text", value = ""),
    fileInput("pdf", label = "PDF file")
  ),
  mainPanel(
    imageOutput("reaction_1", height = "auto"),
    htmlOutput("reaction_desc_1"),
    imageOutput("reaction_2", height = "auto"),
    htmlOutput("reaction_desc_2"),
    imageOutput("reaction_3", height = "auto"),
    htmlOutput("reaction_desc_3"),
    htmlOutput("over_flow")
  )
)

shinyApp(ui = ui, server = server)
